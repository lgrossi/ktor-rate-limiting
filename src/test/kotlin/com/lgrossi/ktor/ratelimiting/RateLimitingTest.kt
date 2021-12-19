package com.lgrossi.ktor.ratelimiting

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.server.testing.*
import java.time.Duration
import kotlin.test.*

class RateLimitingTest {
    @Test
    fun limiterBlocksAllRequestsIfLimitIs0() {
        withTestApplication({
            install(RateLimiting) {
                limit(0)
                duration(Duration.ofMinutes(0))
            }
        }) {
            handleRequest(HttpMethod.Get, "/test").apply {
                assertEquals(HttpStatusCode.TooManyRequests, response.status())
            }
            handleRequest(HttpMethod.Get, "/test").apply {
                assertEquals(HttpStatusCode.TooManyRequests, response.status())
            }
        }
    }

    @Test
    fun limiterBlocksAnyOrdinaryPathsAfterNRequests() {
        withTestApplication({
            install(RateLimiting) {
                limit(100)
                duration(Duration.ofMinutes(1))
            }
        }) {
            repeat(100) {
                handleRequest(HttpMethod.Get, "/test").apply {
                    assertEquals(HttpStatusCode.NotFound, response.status())
                }
            }
            handleRequest(HttpMethod.Get, "/anyOther").apply {
                assertEquals(HttpStatusCode.TooManyRequests, response.status())
            }
        }
    }

    @Test
    fun limiterBlocksNonOptionsMethodsAfterNRequests() {
        withTestApplication({
            install(RateLimiting) {
                limit(1)
                duration(Duration.ofMinutes(1))
            }
        }) {
            handleRequest(HttpMethod.Get, "/test").apply {
                assertEquals(HttpStatusCode.NotFound, response.status())
            }
            HttpMethod.DefaultMethods.filter { it != HttpMethod.Options }.forEach { method ->
                handleRequest(method, "/test").apply {
                    assertEquals(HttpStatusCode.TooManyRequests, response.status())
                }
            }
        }
    }

    @Test
    fun limiterIgnoresByDefaultOptions() {
        withTestApplication({
            install(RateLimiting) {
                limit(1)
                duration(Duration.ofMinutes(1))
            }
        }) {
            handleRequest(HttpMethod.Get, "/test").apply {
                assertEquals(HttpStatusCode.NotFound, response.status())
            }
            handleRequest(HttpMethod.Options, "/test").apply {
                assertEquals(HttpStatusCode.NotFound, response.status())
            }
            handleRequest(HttpMethod.Get, "/test").apply {
                assertEquals(HttpStatusCode.TooManyRequests, response.status())
            }
        }
    }

    @Test
    fun limiterIgnoresByDefaultHealthPath() {
        withTestApplication({
            install(RateLimiting) {
                limit(1)
                duration(Duration.ofMinutes(1))
            }
        }) {
            handleRequest(HttpMethod.Get, "/test").apply {
                assertEquals(HttpStatusCode.NotFound, response.status())
            }
            handleRequest(HttpMethod.Get, "/health").apply {
                assertEquals(HttpStatusCode.NotFound, response.status())
            }
            handleRequest(HttpMethod.Get, "/test").apply {
                assertEquals(HttpStatusCode.TooManyRequests, response.status())
            }
        }
    }

    @Test
    fun limiterIgnoresCustomBypassedMethod() {
        withTestApplication({
            install(RateLimiting) {
                limit(1)
                duration(Duration.ofMinutes(1))
                bypassMethod(HttpMethod.Get)
            }
        }) {
            handleRequest(HttpMethod.Get, "/test").apply {
                assertEquals(HttpStatusCode.NotFound, response.status())
            }
            handleRequest(HttpMethod.Post, "/test").apply {
                assertEquals(HttpStatusCode.NotFound, response.status())
            }
            handleRequest(HttpMethod.Get, "/test").apply {
                assertEquals(HttpStatusCode.NotFound, response.status())
            }
            handleRequest(HttpMethod.Post, "/test").apply {
                assertEquals(HttpStatusCode.TooManyRequests, response.status())
            }
        }
    }

    @Test
    fun limiterIgnoresCustomBypassedPath() {
        withTestApplication({
            install(RateLimiting) {
                limit(1)
                duration(Duration.ofMinutes(1))
                bypassPath("/test")
            }
        }) {
            handleRequest(HttpMethod.Get, "/test").apply {
                assertEquals(HttpStatusCode.NotFound, response.status())
            }
            handleRequest(HttpMethod.Post, "/random").apply {
                assertEquals(HttpStatusCode.NotFound, response.status())
            }
            handleRequest(HttpMethod.Get, "/test").apply {
                assertEquals(HttpStatusCode.NotFound, response.status())
            }
            handleRequest(HttpMethod.Post, "/random").apply {
                assertEquals(HttpStatusCode.TooManyRequests, response.status())
            }
        }
    }

    @Test
    fun usingCustomKeyDeterminerViaCallback() {
        withTestApplication({
            install(RateLimiting) {
                limit(1)
                keyExtractCallback { context -> Pair(context.call.request.origin.method.value, context.call.request.origin.remoteHost) }
            }
        }) {
            handleRequest(HttpMethod.Get, "/test").apply {
                assertEquals(HttpStatusCode.NotFound, response.status())
            }
            handleRequest(HttpMethod.Get, "/test").apply {
                assertEquals(HttpStatusCode.TooManyRequests, response.status())
            }
            handleRequest(HttpMethod.Post, "/test").apply {
                assertEquals(HttpStatusCode.NotFound, response.status())
            }
            handleRequest(HttpMethod.Post, "/test").apply {
                assertEquals(HttpStatusCode.TooManyRequests, response.status())
            }
        }
    }
}