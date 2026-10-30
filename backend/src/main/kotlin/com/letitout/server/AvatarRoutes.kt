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

/** [seconds] is how long this live preview may run, counted on the phone from the first video frame. */
@Serializable internal data class AvatarSessionResponse(val sessionToken: String, val seconds: Long)

private val avatarJson = Json { ignoreUnknownKeys = true }
private val avatarHttp = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(7)).build()

private fun avatarReady(config: ServerConfig): Boolean =
    config.chatEnabled && config.simliApiKey != null && config.simliFaceId != null &&
        com.google.firebase.FirebaseApp.getApps().isNotEmpty()

/** Only the server knows the face ID and API key. The client receives a short-lived session token. */
private suspend fun simliSessionToken(config: ServerConfig, mood: String, seconds: Long): String = withContext(Dispatchers.IO) {
    val body = buildJsonObject {
        put("faceId", requireNotNull(config.simliFaceId))
        put("apiVersion", "v2")
        put("handleSilence", true)
        // A little over the preview so connecting doesn't eat into it; Simli ends the session here regardless.
        put("maxSessionLength", seconds + 20)
        put("maxIdleTime", 120)
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
    val createSession: suspend (ServerConfig, String, Long) -> String = ::simliSessionToken,
    val rateLimiter: (ServerConfig) -> RateLimiter = { config ->
        if (config.production) FirestoreRateLimiter(3, 60, "avatar_rate_limits")
        else LocalRateLimiter(3, 60)
    }
)

internal fun Route.registerAvatarRoutes(
    config: ServerConfig,
    dependencies: AvatarDependencies,
    trial: () -> TrialStore,
    policy: TrialPolicy,
    clock: () -> Long
) {
    val limiter: RateLimiter by lazy { dependencies.rateLimiter(config) }
    // The app calls this when Ari's video first appears; only then does the week's preview count.
    post("/v1/avatar/started") {
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
        val sessionId = call.request.queryParameters["sessionId"]
        if (!validSessionId(sessionId)) return@post call.respond(HttpStatusCode.BadRequest)
        val confirmed = try {
            trial().update(uid) { state -> policy.confirmLive(state, requireNotNull(sessionId), clock()) }
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
            return@post call.respond(HttpStatusCode.ServiceUnavailable)
        }
        call.respond(if (confirmed) HttpStatusCode.NoContent else HttpStatusCode.NotFound)
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
        val sessionId = call.request.queryParameters["sessionId"]
        if (!validSessionId(sessionId)) return@post call.respond(HttpStatusCode.BadRequest)
        val gate = try {
            trial().update(uid) { state -> policy.live(state, requireNotNull(sessionId), clock()) }
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
            return@post call.respond(HttpStatusCode.ServiceUnavailable)
        }
        val seconds = when (gate) {
            is LiveGate.Open -> gate.seconds
            is LiveGate.Used -> return@post call.respondText(
                avatarJson.encodeToString(ErrorResponse("You've had this week's live preview.", "live_used", gate.nextAt * 1000)),
                ContentType.Application.Json, HttpStatusCode.Forbidden
            )
            LiveGate.DailyLimit -> return@post call.respondText(
                avatarJson.encodeToString(ErrorResponse("You've had today's free chats.", "daily_limit")),
                ContentType.Application.Json, HttpStatusCode.Forbidden
            )
            LiveGate.SessionEnded -> return@post call.respondText(
                avatarJson.encodeToString(ErrorResponse("This chat's time is up.", "session_ended")),
                ContentType.Application.Json, HttpStatusCode.Forbidden
            )
        }
        try {
            val mood = call.request.queryParameters["mood"]?.uppercase()
                ?.takeIf { it in setOf("ANGRY", "SAD", "STRESSED", "ANXIOUS", "LONELY", "HAPPY", "TALK") }
                ?: "TALK"
            val token = dependencies.createSession(config, mood, seconds)
            call.respondText(avatarJson.encodeToString(AvatarSessionResponse(token, seconds)), ContentType.Application.Json)
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
            call.respond(HttpStatusCode.BadGateway)
        }
    }
}
