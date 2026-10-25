package com.letitout.server

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
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

/** Only the server knows the face ID and API key. The client receives a short-lived session token. */
private suspend fun simliSessionToken(config: ServerConfig): String = withContext(Dispatchers.IO) {
    val body = buildJsonObject {
        put("faceId", requireNotNull(config.simliFaceId))
        put("apiVersion", "v2")
        put("handleSilence", true)
        put("maxSessionLength", 300)
        put("maxIdleTime", 60)
        put("audioInputFormat", "pcm16")
        put("streaming_width", 512)
        put("streaming_height", 512)
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
    val createSession: suspend (ServerConfig) -> String = ::simliSessionToken,
    val rateLimiter: (ServerConfig) -> RateLimiter = { config ->
        if (config.production) FirestoreRateLimiter(3, 60, "avatar_rate_limits")
        else LocalRateLimiter(3, 60)
    }
)

internal fun Route.registerAvatarRoutes(config: ServerConfig, dependencies: AvatarDependencies) {
    val limiter: RateLimiter by lazy { dependencies.rateLimiter(config) }
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
            val token = dependencies.createSession(config)
            call.respondText(avatarJson.encodeToString(AvatarSessionResponse(token)), ContentType.Application.Json)
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
            call.respond(HttpStatusCode.BadGateway)
        }
    }
}
