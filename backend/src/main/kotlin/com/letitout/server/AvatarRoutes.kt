package com.letitout.server

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.origin
import io.ktor.server.response.*
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@Serializable internal data class AvatarSessionResponse(val sessionToken: String)

private val avatarJson = Json { ignoreUnknownKeys = true }
private val avatarHttp = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(7)).build()

private fun avatarReady(config: ServerConfig): Boolean =
    config.chatEnabled && config.simliApiKey != null && config.simliFaceId != null &&
        com.google.firebase.FirebaseApp.getApps().isNotEmpty()

/** The website greeting needs no chat or sign-in, but production limits are shared through Firestore. */
private fun webAvatarReady(config: ServerConfig): Boolean =
    config.webAvatarOrigins.isNotEmpty() && config.simliApiKey != null && config.simliFaceId != null &&
        (!config.production || com.google.firebase.FirebaseApp.getApps().isNotEmpty())

/** Only the server knows the face ID and API key. The client receives a short-lived session token. */
private suspend fun simliSessionToken(
    config: ServerConfig,
    mood: String,
    maxSessionLength: Int = 300,
    maxIdleTime: Int = 120
): String = withContext(Dispatchers.IO) {
    val body = buildJsonObject {
        put("faceId", requireNotNull(config.simliFaceId))
        put("apiVersion", "v2")
        put("handleSilence", true)
        put("maxSessionLength", maxSessionLength)
        put("maxIdleTime", maxIdleTime)
        put("audioInputFormat", "pcm16")
        put("streaming_width", 576)
        put("streaming_height", 1024)
        config.simliBackgroundIds[mood]?.let { put("background_id", it) }
    }
    val request = HttpRequest.newBuilder(URI.create("https://api.simli.ai/compose/token"))
        .header("x-simli-api-key", requireNotNull(config.simliApiKey))
        .header("Content-Type", "application/json")
        .timeout(Duration.ofSeconds(12))
        .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
        .build()
    val response = avatarHttp.send(request, HttpResponse.BodyHandlers.ofString())
    if (response.statusCode() !in 200..299) throw IllegalStateException("Avatar provider unavailable")
    avatarJson.parseToJsonElement(response.body()).jsonObject["session_token"]
        ?.jsonPrimitive?.content?.takeIf { it.isNotBlank() && it.length < 8192 }
        ?: throw IllegalStateException("Avatar provider returned no session token")
}

internal data class AvatarDependencies(
    val ready: (ServerConfig) -> Boolean = ::avatarReady,
    val verifyUid: suspend (String, ServerConfig) -> String = ::verifiedUid,
    val createSession: suspend (ServerConfig, String) -> String = ::simliSessionToken,
    val rateLimiter: (ServerConfig) -> RateLimiter = { config ->
        if (config.production) FirestoreRateLimiter(3, 60, "avatar_rate_limits")
        else LocalRateLimiter(3, 60)
    },
    val webReady: (ServerConfig) -> Boolean = ::webAvatarReady,
    val createWebSession: suspend (ServerConfig) -> String = { config ->
        simliSessionToken(config, "TALK", maxSessionLength = 60, maxIdleTime = 15)
    },
    val webRateLimiter: (ServerConfig) -> RateLimiter = { config ->
        if (config.production) FirestoreRateLimiter(2, config.webAvatarGlobalHourlyLimit, "web_avatar_rate_limits")
        else LocalRateLimiter(2, config.webAvatarGlobalHourlyLimit)
    }
)

/** Cloud Run appends the connecting address last, so a client-supplied X-Forwarded-For cannot replace it. */
private fun ApplicationCall.clientAddress(): String =
    request.headers["X-Forwarded-For"]?.split(',')?.map { it.trim() }?.lastOrNull { it.isNotEmpty() }
        ?: request.origin.remoteAddress

internal fun Route.registerAvatarRoutes(config: ServerConfig, dependencies: AvatarDependencies) {
    val limiter: RateLimiter by lazy { dependencies.rateLimiter(config) }
    val webLimiter: RateLimiter by lazy { dependencies.webRateLimiter(config) }

    // One-minute scripted greeting for the website: no chat, no user text, and a token only for allowed origins.
    post("/v1/avatar/web-session") {
        call.response.header(HttpHeaders.CacheControl, "no-store")
        val origin = call.request.headers[HttpHeaders.Origin]?.trimEnd('/')
        if (origin == null || origin !in config.webAvatarOrigins) return@post call.respond(HttpStatusCode.Forbidden)
        call.response.header(HttpHeaders.AccessControlAllowOrigin, origin)
        call.response.header(HttpHeaders.Vary, HttpHeaders.Origin)
        if (!dependencies.webReady(config)) return@post call.respond(HttpStatusCode.ServiceUnavailable)
        val allowed = try {
            webLimiter.allow("ip:${call.clientAddress()}")
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
            return@post call.respond(HttpStatusCode.ServiceUnavailable)
        }
        if (!allowed) return@post call.respond(HttpStatusCode.TooManyRequests)
        try {
            val token = dependencies.createWebSession(config)
            call.respondText(avatarJson.encodeToString(AvatarSessionResponse(token)), ContentType.Application.Json)
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
            call.respond(HttpStatusCode.BadGateway)
        }
    }

    post("/v1/avatar/session") {
        call.response.header(HttpHeaders.CacheControl, "no-store")
        if (!dependencies.ready(config)) return@post call.respond(HttpStatusCode.ServiceUnavailable)
        val idToken = call.request.headers[HttpHeaders.Authorization]
            ?.takeIf { it.startsWith("Bearer ") }?.removePrefix("Bearer ")
        if (idToken.isNullOrBlank()) return@post call.respond(HttpStatusCode.Unauthorized)
        val uid = try {
            dependencies.verifyUid(idToken, config)
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
            return@post call.respond(HttpStatusCode.Unauthorized)
        }
        val allowed = try {
            limiter.allow(uid)
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
            return@post call.respond(HttpStatusCode.ServiceUnavailable)
        }
        if (!allowed) return@post call.respond(HttpStatusCode.TooManyRequests)
        try {
            val mood = call.request.queryParameters["mood"]?.uppercase()
                ?.takeIf { it in setOf("ANGRY", "SAD", "STRESSED", "ANXIOUS", "LONELY", "HAPPY", "TALK") }
                ?: "TALK"
            val token = dependencies.createSession(config, mood)
            call.respondText(avatarJson.encodeToString(AvatarSessionResponse(token)), ContentType.Application.Json)
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
            call.respond(HttpStatusCode.BadGateway)
        }
    }
}
