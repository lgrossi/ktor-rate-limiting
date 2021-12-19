package com.lgrossi.ktor.ratelimiting

import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class RateLimiter<in T : Any>(
    private val limit: Long = 1000,
    private val duration: Duration = Duration.ofHours(1),
    private val cleanupInterval: Long = 20,
    numberOfRecords: Int = 64
) {
    private val records: ConcurrentMap<T, Rate> = ConcurrentHashMap(numberOfRecords)
    private var lastCleanup = Instant.now()

    fun consume(key: T): Rate {
        records.compute(key) { _, rate: Rate? ->
            val updatedRate = rate ?: Rate.new(RateConfig(duration, limit))
            updatedRate.renew().consume()
        }

        deleteExpiredRecords()

        return records[key]!!
    }

    fun allow(key: T): Boolean = limit > 0 && (!isDepleted(key) || isExpired(key))

    operator fun get(key: T): Rate = records.getOrDefault(key, Rate.new(RateConfig(duration, limit)))

    private fun isExpired(key: T): Boolean = records[key]?.isExpired() ?: true

    private fun isDepleted(key: T): Boolean = records[key]?.isDepleted() ?: false

    private fun deleteExpiredRecords() {
        if (Duration.between(lastCleanup, Instant.now()) > Duration.ofMinutes(cleanupInterval)) {
            records.values.removeAll(records.filter { it.value.isExpired() }.values.toSet())
            lastCleanup = Instant.now()
        }
    }

    companion object {
        /**
         * Request limit per hour
         */
        const val HEADER_LIMIT = "X-RateLimit-Limit"

        /**
         * The number of requests left in the current time window
         */
        const val HEADER_REMAINING = "X-RateLimit-Remaining"

        /**
         * UNIX epoch second timestamp at which the request quota is reset
         */
        const val HEADER_RESET = "X-RateLimit-Reset"

        /**
         * Indicate how long to wait before the client should make a
         * new attempt to connect. Unlike [HEADER_RESET], this is expressed
         * as seconds in _relative_ time from the current time.
         */
        const val HEADER_RETRY = "Retry-After"
    }
}