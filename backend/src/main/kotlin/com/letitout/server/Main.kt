package com.letitout.server

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.readByteArray
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable data class Message(val role: String, val content: String)
@Serializable data class ChatRequest(val mood: String, val messages: List<Message>)
@Serializable data class ChatResponse(val reply: String, val safetyState: String = NORMAL)
@Serializable data class ErrorResponse(val error: String)

const val NORMAL = "NORMAL"
const val SUPPORT_SUGGESTED = "SUPPORT_SUGGESTED"
internal const val MAX_BODY_CHARS = 64_000
private const val MAX_BODY_BYTES = MAX_BODY_CHARS * 4L
private const val HOUR_MILLIS = 60 * 60 * 1000L

private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
private val supportedMoods = setOf("ANGRY", "SAD", "STRESSED", "ANXIOUS", "LONELY", "HAPPY", "TALK")

internal object FriendlyErrors {
    const val UNAVAILABLE = "Chat isn't available right now. Please try again a little later."
    const val SIGN_IN = "Please sign in again to keep talking."
    const val TOO_LONG = "That's a lot to send in one go. Try a shorter message."
    const val INVALID = "That message didn't come through properly. Please try sending it again."
    const val TOO_MANY = "You've sent a lot of messages in the last hour. Take a breather and come back in a little while."
    const val BUSY = "Lots of people are talking right now. Give it a minute and try again."
    const val NO_REPLY = "Couldn't get a reply just now. Please try sending that again."
}

class ChatDeps(
    val verifyToken: (suspend (String) -> String?)?,
    val provider: ReplyProvider?,
    val limiter: RateLimiter = RateLimiter(20, HOUR_MILLIS),
    val careLimiter: RateLimiter = RateLimiter(30, HOUR_MILLIS)
) {
    companion object {
        fun fromEnvironment(): ChatDeps {
            val verifier: (suspend (String) -> String?)? = if (setupFirebase()) ::verifyFirebaseToken else null
            val provider = GroqProvider.fromEnvironment()
            if (provider == null) System.err.println("GROQ_API_KEY is not set. AI chat remains unavailable.")
            return ChatDeps(verifier, provider)
        }
    }
}

private suspend fun verifyFirebaseToken(token: String): String? =
    withContext(Dispatchers.IO) { FirebaseAuth.getInstance().verifyIdToken(token).uid }

private fun setupFirebase(): Boolean {
    if (FirebaseApp.getApps().isNotEmpty()) return true
    val projectId = System.getenv("FIREBASE_PROJECT_ID")
    if (projectId.isNullOrBlank()) {
        System.err.println("FIREBASE_PROJECT_ID is not set. AI chat remains unavailable.")
        return false
    }
    return runCatching {
        val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.getApplicationDefault())
            .setProjectId(projectId)
            .build()
        FirebaseApp.initializeApp(options)
    }.onFailure { System.err.println("Firebase admin initialization failed. AI chat remains unavailable.") }.isSuccess
}

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, host = "0.0.0.0", port = port) { module() }.start(wait = true)
}

fun Application.module(deps: ChatDeps = ChatDeps.fromEnvironment()) {
    routing {
        get("/health") { call.respondText("ok") }
        post("/v1/chat") { call.handleChat(deps) }
    }
}

private suspend fun ApplicationCall.respondError(status: HttpStatusCode, message: String) =
    respondText(json.encodeToString(ErrorResponse.serializer(), ErrorResponse(message)), ContentType.Application.Json, status)

private suspend fun ApplicationCall.respondReply(reply: String, safetyState: String) =
    respondText(json.encodeToString(ChatResponse.serializer(), ChatResponse(reply, safetyState)), ContentType.Application.Json)

private fun ChatRequest.isValid(): Boolean =
    mood in supportedMoods && messages.size in 1..12 && messages.last().role == "user" &&
        messages.all { it.role in setOf("user", "assistant") && it.content.length in 1..2000 }

/** Reads at most [MAX_BODY_BYTES]; null means the body is too large. */
private suspend fun ApplicationCall.receiveLimitedText(): String? {
    if ((request.headers[HttpHeaders.ContentLength]?.toLongOrNull() ?: 0L) > MAX_BODY_BYTES) return null
    val bytes = receiveChannel().readRemaining(MAX_BODY_BYTES + 1).readByteArray()
    if (bytes.size > MAX_BODY_BYTES) return null
    return bytes.decodeToString().takeIf { it.length <= MAX_BODY_CHARS }
}

private suspend fun ApplicationCall.handleChat(deps: ChatDeps) {
    val verifyToken = deps.verifyToken
    val provider = deps.provider
    if (verifyToken == null || provider == null) return respondError(HttpStatusCode.ServiceUnavailable, FriendlyErrors.UNAVAILABLE)

    val idToken = request.headers[HttpHeaders.Authorization]
        ?.takeIf { it.startsWith("Bearer ", ignoreCase = true) }?.substring(7)?.trim()
    if (idToken.isNullOrEmpty()) return respondError(HttpStatusCode.Unauthorized, FriendlyErrors.SIGN_IN)
    val uid = try { verifyToken(idToken) } catch (e: CancellationException) { throw e } catch (_: Exception) { null }
    if (uid.isNullOrBlank()) return respondError(HttpStatusCode.Unauthorized, FriendlyErrors.SIGN_IN)

    val bodyText = receiveLimitedText() ?: return respondError(HttpStatusCode.PayloadTooLarge, FriendlyErrors.TOO_LONG)
    val req = try { json.decodeFromString(ChatRequest.serializer(), bodyText) } catch (_: Exception) { null }
    if (req == null || !req.isValid()) return respondError(HttpStatusCode.BadRequest, FriendlyErrors.INVALID)

    val recentUserMessages = req.messages.filter { it.role == "user" }.takeLast(3).map { it.content }
    val care = needsExtraCare(recentUserMessages)
    val fallback = Persona.fallbackReply(req.messages.last().content)
    if (care) {
        if (!deps.careLimiter.tryAcquire(uid)) return respondReply(fallback, SUPPORT_SUGGESTED)
    } else if (!deps.limiter.tryAcquire(uid)) {
        return respondError(HttpStatusCode.TooManyRequests, FriendlyErrors.TOO_MANY)
    }

    val reply = try {
        polishReply(provider.reply(Persona.systemPrompt(req.mood, care), req.messages.takeLast(12)))
            .ifBlank { throw ProviderUnavailable("blank reply") }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        return when {
            care -> respondReply(fallback, SUPPORT_SUGGESTED)
            e is ProviderRateLimited -> respondError(HttpStatusCode.TooManyRequests, FriendlyErrors.BUSY)
            else -> respondError(HttpStatusCode.BadGateway, FriendlyErrors.NO_REPLY)
        }
    }
    respondReply(reply, if (care || mentionsProfessionalHelp(reply)) SUPPORT_SUGGESTED else NORMAL)
}
