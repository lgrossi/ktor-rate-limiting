# Ktor RateLimiting Plugin
Very simple [Ktor](https://ktor.io) plugin for rate limiting in ktor http apis.

The number of requests from a same client key (ip by default but can be customised based on the ktor context) is limited to a pre-defined value. If a client key exceeds the limit, the API returns [429 - Too Many Requests](https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/429).

**v1.0.0 Features**
- Customisable limit and duration
- Customisable bypassed methods and paths
- Customisable limiter key callback based on ktor context

## Installation

Add the github repository and the plugin dependency in your `build.gradle.kts` file
```kotlin
repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/lgrossi/ktor-rate-limiting")
        credentials {
            username = "lgrossi"
            password = "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"
        }
    }
}

dependencies {
    ...
    implementation("com.lgrossi:ktor-rate-limiting:1.0.0")
}
```
The token shared above is an access token for github packages. It's read-only, so it can be safely shared with others. Normally, sharing your secrets and tokens is not a good practice, but github still doesn't have unauthenticated reader for their packages.

Maven pom.xml
```xml
<dependency>
    <groupId>com.lgrossi</groupId>
    <artifactId>ktor-rate-limiting</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Usage

#### Basic installation with preset configurations
```kotlin
fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configureRouting()
        install(RateLimiting)
    }.start(wait = true)
}
```

#### Define limit and duration for a custom request rate
```kotlin
fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configureRouting()
        install(RateLimiting) {
            limit(200)
            duration(Duration.ofSeconds(10))
        }
    }.start(wait = true)
}
```
**limit** - the amount of request allowed in a period of time  
**duration** - a time period within which your limit is applied, after that your limit is renewed.

#### Bypass methods and paths that you don't want to limit
```kotlin
fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configureRouting()
        install(RateLimiting) {
            bypassMethod(HttpMethod.Post)
            bypassPath("/health-check")
        }
    }.start(wait = true)
}
```
**bypassMethod** - sets the limiter to bypass a method, allowing 
**duration** - a time period within which your limit is applied, after that your limit is renewed.

#### Custom key extractor for more complex limiting
In this example below we are limiting per <Address, Method> pair, so that a single user can perform N requests in a given time for each available HTTP method.
```kotlin
fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configureRouting()
        install(RateLimiting) {
            keyExtractCallback { context -> Pair(context.call.request.origin.method.value, context.call.request.origin.remoteHost) }
        }
    }.start(wait = true)
}
```
**keyExtractCallback** - a callback function that determines the key used by the rate limit to identify the user. The call back expects a Ktor context and returns of type any.