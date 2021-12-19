package com.lgrossi.ktor.ratelimiting

import java.time.Duration
import java.time.Instant

data class RateConfig constructor(val duration: Duration, val limit: Long)

data class Rate private constructor(
    val requests: Long,
    val resetsAt: Instant,
    val rateConfig: RateConfig,
) {
    fun getRemainingTime(): Long = (resetsAt.epochSecond - Instant.now().epochSecond).coerceAtLeast(0)

    fun isDepleted(): Boolean = requests <= 0
    fun isExpired(time: Instant = Instant.now()): Boolean = resetsAt <= time

    fun renew(): Rate = if (!isExpired()) this else new(rateConfig)
    fun consume(): Rate = if (isDepleted()) this else this.copy(requests = requests - 1)

    companion object {
        fun new(rateConfig: RateConfig): Rate {
            return Rate(rateConfig.limit, Instant.now().plus(rateConfig.duration), rateConfig)
        }
    }
}