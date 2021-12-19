package com.lgrossi.ktor.ratelimiting

import kotlin.test.*
import java.time.Duration

class RateTest {
    @Test
    fun getRemainingTime() {
        assertEquals(0, Rate.new(RateConfig(Duration.ofSeconds(0), 1)).getRemainingTime())
        assertEquals(1, Rate.new(RateConfig(Duration.ofSeconds(1), 1)).getRemainingTime())
        assertEquals(60, Rate.new(RateConfig(Duration.ofMinutes(1), 1)).getRemainingTime())
        assertEquals(3600, Rate.new(RateConfig(Duration.ofHours(1), 1)).getRemainingTime())
    }

    @Test
    fun isDepleted() {
        var rate = Rate.new(RateConfig(Duration.ofSeconds(1), 100))
        repeat(100) {
            assertFalse { rate.isDepleted() }
            rate = rate.consume()
        }
        assertTrue { rate.isDepleted() }
    }

    @Test
    fun isExpired() {
        assertTrue { Rate.new(RateConfig(Duration.ofSeconds(0), 100)).isExpired() }
        assertFalse { (Rate.new(RateConfig(Duration.ofSeconds(1), 100)).isExpired()) }
    }

    @Test
    fun renewDoesntWorkIfNotExpired() {
        var rate = Rate.new(RateConfig(Duration.ofSeconds(1), 100))
        repeat(100) {
            rate = rate.consume()
        }
        rate.renew()
        assertTrue { rate.isDepleted() }
    }

    @Test
    fun renewResetsExpireRates() {
        var rate = Rate.new(RateConfig(Duration.ofSeconds(0), 100))
        repeat(100) {
            rate = rate.consume()
        }
        rate.renew()
        assertTrue { rate.isDepleted() }
    }
}