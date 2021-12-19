package com.lgrossi.ktor.ratelimiting

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import java.time.Duration

class RateLimiting private constructor(configuration: Configuration) {
    private val rateLimit = RateLimiter<Any>(configuration.limit, configuration.duration)

    private val keyExtractor = configuration.keyExtractCallback
    private val bypassedPaths = configuration.bypassedPaths + Configuration.LimiterDefaultBypassedPaths
    private val bypassedMethods = configuration.bypassedMethods + Configuration.LimiterDefaultBypassedMethods

    private fun bypass(method: HttpMethod, path: String): Boolean {
        return method in bypassedMethods || path in bypassedPaths
    }

    private fun consume(key: Any): Pair<Rate, Boolean> {
        if (!rateLimit.allow(key)) {
            return Pair(rateLimit[key], false)
        }

        rateLimit.consume(key)

        return Pair(rateLimit[key], true)
    }

    private suspend fun ApplicationCall.respondLimiterFailed(rate: Rate) {
        response.header(RateLimiter.HEADER_RETRY, rate.getRemainingTime())
        respond(HttpStatusCode.TooManyRequests)
    }

    private fun ApplicationCall.setLimiterHeaders(rate: Rate) {
        response.header(RateLimiter.HEADER_LIMIT, rate.rateConfig.limit)
        response.header(RateLimiter.HEADER_REMAINING, rate.requests)
        response.header(RateLimiter.HEADER_RESET, rate.resetsAt.epochSecond)
    }

    suspend fun intercept(context: PipelineContext<Unit, ApplicationCall>) {
        val call = context.call

        val method: HttpMethod = call.request.httpMethod
        val path: String = call.request.uri

        if (bypass(method, path)) {
            context.proceed()
            return
        }

        val (rate, consumed) = consume(keyExtractor(context))

        call.setLimiterHeaders(rate)

        if (!consumed) {
            call.respondLimiterFailed(rate)
            context.finish()
            return
        }

        context.proceed()
    }

    class Configuration {
        var limit: Long = 1000L

        var duration: Duration = Duration.ofHours(1L)

        var keyExtractCallback: (context: PipelineContext<Unit, ApplicationCall>) -> Any = {
            context -> context.call.request.origin.remoteHost
        }

        val bypassedMethods: MutableSet<HttpMethod> = HashSet()

        val bypassedPaths: MutableSet<String> = mutableSetOf()

        fun limit(numberOfRequests: Long) {
            limit = numberOfRequests
        }

        fun duration(duration: Duration) {
            this.duration = duration
        }

        fun bypassMethod(method: HttpMethod) {
            bypassedMethods.add(method)
        }

        fun bypassPath(path: String) {
            bypassedPaths.add(path)
        }

        fun keyExtractCallback(extractor: (context: PipelineContext<Unit, ApplicationCall>) -> Any) {
            keyExtractCallback = extractor
        }

        companion object {
            val LimiterDefaultBypassedMethods: Set<HttpMethod> = setOf(HttpMethod.Options)
            val LimiterDefaultBypassedPaths: Set<String> = setOf("/health")
        }
    }

    companion object Feature : ApplicationFeature<ApplicationCallPipeline, Configuration, RateLimiting> {
        override val key: AttributeKey<RateLimiting> = AttributeKey("RateLimiting")

        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): RateLimiting {
            val rateLimiting = RateLimiting(Configuration().apply(configure))
            pipeline.intercept(ApplicationCallPipeline.Features) { rateLimiting.intercept(this) }
            return rateLimiting
        }
    }
}