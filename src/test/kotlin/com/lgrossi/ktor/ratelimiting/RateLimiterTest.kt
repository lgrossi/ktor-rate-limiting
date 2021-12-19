package com.lgrossi.ktor.ratelimiting

import kotlin.test.*
import java.time.Duration
import kotlin.random.Random
import kotlin.random.nextUInt

class RateLimiterTest {
    @Test
    fun emptyLimiterAllowRandomKeys() {
        val limiter = RateLimiter<UInt>(10, Duration.ofMinutes(1))
        assertTrue { limiter.allow(Random.nextUInt()) }
        assertTrue { limiter.allow(Random.nextUInt()) }
        assertTrue { limiter.allow(Random.nextUInt()) }
    }

    @Test
    fun limitZeroAlwaysBlock() {
        val limiter = RateLimiter<UInt>(0, Duration.ofMinutes(1))
        assertFalse { limiter.allow(Random.nextUInt()) }
        assertFalse { limiter.allow(Random.nextUInt()) }
        assertFalse { limiter.allow(Random.nextUInt()) }
    }

    @Test
    fun blocksIfLimitIsReached() {
        val limiter = RateLimiter<Int>(100, Duration.ofMinutes(1))
        repeat(100) {
            assertTrue { limiter.allow(1) }
            limiter.consume(1)
        }
        assertFalse { limiter.allow(1) }
    }

    @Test
    fun allowsAgainAfterExpiration() {
        val limiter = RateLimiter<Int>(100, Duration.ofMinutes(0))
        repeat(100) {
            limiter.consume(1)
        }
        assertTrue { limiter.allow(1) }
    }

    @Test
    fun consumptionOfNewKeyInsertsProperValue() {
        val limiter = RateLimiter<Int>(100, Duration.ofMinutes(1))
        assertNotSame(limiter[1], limiter[1])
        limiter.consume(1)
        assertSame(limiter[1], limiter[1])
    }
}