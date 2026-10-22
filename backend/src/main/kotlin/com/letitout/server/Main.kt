package com.letitout.server

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.AuthErrorCode
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
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
import java.time.Instant

@Serializable data class Message(val role: String, val content: String)
@Serializable data class ChatRequest(val mood: String, val messages: List<Message>)
@Serializable data class ChatResponse(val reply: String, val safetyState: String = NORMAL)
@Serializable data class ErrorResponse(val error: String)

const val NORMAL = "NORMAL"
const val SUPPORT_SUGGESTED = "SUPPORT_SUGGESTED"
internal const val MAX_BODY_CHARS = 64_000
internal const val MAX_REPORT_CHARS = 8_000
private const val HOUR_MILLIS = 60 * 60 * 1000L
private const val GLOBAL_KEY = "all"
// A rewrite only when the first call was quick, so two calls stay inside the app's 45 s read timeout.
private const val REWRITE_BUDGET_MILLIS = 10_000L

private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
private val supportedMoods = setOf("ANGRY", "SAD", "STRESSED", "ANXIOUS", "LONELY", "HAPPY", "TALK")

internal object FriendlyErrors {
    const val UNAVAILABLE = "Chat isn't available right now. Please try again a little later."
    const val SIGN_IN = "Please sign in again to keep talking."
    const val TOO_LONG = "That's a lot to send in one go. Try a shorter message."
    const val INVALID = "That message didn't come through properly. Please try sending it again."
    const val TOO_MANY = "That's the message limit for this hour. Give it a little while, then we can keep going."
    const val BUSY = "Lots of people are talking right now. Give it a minute and try again."
    const val NO_REPLY = "Couldn't get a reply just now. Please try sending that again."
    const val TOO_MANY_REPORTS = "Thanks, we already have your recent reports. Please try again a little later."
}

class ChatDeps(
    val verifyToken: (suspend (String) -> String?)?,
    val provider: ReplyProvider?,
    val limiter: RateLimiter = RateLimiter(20, HOUR_MILLIS),
    val careLimiter: RateLimiter = RateLimiter(30, HOUR_MILLIS),
    /** Provider calls per minute across all users, so rotating guest accounts cannot run up unbounded spend. */
    val globalLimiter: RateLimiter = RateLimiter(60, 60_000L),
    val reportSink: ReportSink = FileReportSink.fromEnvironment(),
    val reportLimiter: RateLimiter = RateLimiter(20, HOUR_MILLIS),
    /** Reports per hour across all users, so rotating guest accounts cannot fill the disk. */
    val reportGlobalLimiter: RateLimiter = RateLimiter(300, HOUR_MILLIS)
) {
    companion object {
        fun fromEnvironment(): ChatDeps {
            val verifier: (suspend (String) -> String?)? = if (setupFirebase()) ::verifyFirebaseToken else null
            val provider = GroqProvider.fromEnvironment()
            if (provider == null) System.err.println("GROQ_API_KEY is not set. AI chat remains unavailable.")
            val perMinute = System.getenv("GLOBAL_REQUESTS_PER_MINUTE")?.trim()?.toIntOrNull()?.takeIf { it > 0 } ?: 60
            return ChatDeps(verifier, provider, globalLimiter = RateLimiter(perMinute, 60_000L))
        }
    }
}

private val rejectedTokenCodes = setOf(
    AuthErrorCode.EXPIRED_ID_TOKEN, AuthErrorCode.INVALID_ID_TOKEN, AuthErrorCode.REVOKED_ID_TOKEN,
    AuthErrorCode.USER_DISABLED, AuthErrorCode.TENANT_ID_MISMATCH
)

/** uid for a good token, null for a bad one; throws when Firebase cannot check it (e.g. its public keys failed to load). */
private suspend fun verifyFirebaseToken(token: String): String? = withContext(Dispatchers.IO) {
    try {
        FirebaseAuth.getInstance().verifyIdToken(token).uid
    } catch (e: FirebaseAuthException) {
        if (e.authErrorCode in rejectedTokenCodes) null else throw e
    } catch (_: IllegalArgumentException) {
        null
    }
}

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
        post("/v1/report") { call.handleReport(deps) }
    }
}

private suspend fun ApplicationCall.respondError(status: HttpStatusCode, message: String) =
    respondText(json.encodeToString(ErrorResponse.serializer(), ErrorResponse(message)), ContentType.Application.Json, status)

private suspend fun ApplicationCall.respondReply(reply: String, safetyState: String) =
    respondText(json.encodeToString(ChatResponse.serializer(), ChatResponse(reply, safetyState)), ContentType.Application.Json)

private fun ChatRequest.isValid(): Boolean =
    mood in supportedMoods && messages.size in 1..12 && messages.last().role == "user" &&
        messages.all { it.role in setOf("user", "assistant") && it.content.length in 1..2000 }

/** Reads at most [maxChars] characters (and 4 bytes per character); null means the body is too large. */
private suspend fun ApplicationCall.receiveLimitedText(maxChars: Int = MAX_BODY_CHARS): String? {
    val maxBytes = maxChars * 4L
    if ((request.headers[HttpHeaders.ContentLength]?.toLongOrNull() ?: 0L) > maxBytes) return null
    val bytes = receiveChannel().readRemaining(maxBytes + 1).readByteArray()
    if (bytes.size > maxBytes) return null
    return bytes.decodeToString().takeIf { it.length <= maxChars }
}

/** The caller's uid, or null after responding 401 (bad sign-in) or 503 (Firebase could not check it). */
private suspend fun ApplicationCall.authenticatedUid(verifyToken: suspend (String) -> String?): String? {
    val idToken = request.headers[HttpHeaders.Authorization]
        ?.takeIf { it.startsWith("Bearer ", ignoreCase = true) }?.substring(7)?.trim()
    if (idToken.isNullOrEmpty()) return null.also { respondError(HttpStatusCode.Unauthorized, FriendlyErrors.SIGN_IN) }
    // A throwing verifier is an outage on our side, not a bad sign-in, so don't send people to sign out.
    val uid = try { verifyToken(idToken) } catch (e: CancellationException) { throw e } catch (_: Exception) {
        return null.also { respondError(HttpStatusCode.ServiceUnavailable, FriendlyErrors.UNAVAILABLE) }
    }
    if (uid.isNullOrBlank()) return null.also { respondError(HttpStatusCode.Unauthorized, FriendlyErrors.SIGN_IN) }
    return uid
}

private suspend fun ApplicationCall.handleChat(deps: ChatDeps) {
    val verifyToken = deps.verifyToken
    val provider = deps.provider
    if (verifyToken == null || provider == null) return respondError(HttpStatusCode.ServiceUnavailable, FriendlyErrors.UNAVAILABLE)
    val uid = authenticatedUid(verifyToken) ?: return

    val bodyText = receiveLimitedText() ?: return respondError(HttpStatusCode.PayloadTooLarge, FriendlyErrors.TOO_LONG)
    val req = try { json.decodeFromString(ChatRequest.serializer(), bodyText) } catch (_: Exception) { null }
    if (req == null || !req.isValid()) return respondError(HttpStatusCode.BadRequest, FriendlyErrors.INVALID)

    val recentUserMessages = req.messages.filter { it.role == "user" }.takeLast(3).map { it.content }
    val recentReplies = req.messages.filter { it.role == "assistant" }.map { it.content }
    val level = careLevel(recentUserMessages)
    // A recent Tele-MANAS mention means the chat already turned heavy, even if the last words slipped past the phrase list.
    var care = level == CareLevel.HARD || recentReplies.takeLast(2).any(::mentionsHelpline)
    // Softer signals get the same gentle prompt and are never refused, but the card only follows a reply that suggests help.
    val gentle = care || level == CareLevel.SOFT
    val fallback = Persona.fallbackReply(req.messages.last().content, recentReplies)
    // Gentle turns may borrow normal quota before falling back to a canned reply.
    val charged = (if (gentle) listOf(deps.careLimiter, deps.limiter) else listOf(deps.limiter)).firstOrNull { it.tryAcquire(uid) }
        ?: return if (gentle) respondReply(fallback, SUPPORT_SUGGESTED)
            else respondError(HttpStatusCode.TooManyRequests, FriendlyErrors.TOO_MANY)
    if (!deps.globalLimiter.tryAcquire(GLOBAL_KEY)) {
        charged.release(uid)
        return if (gentle) respondReply(fallback, SUPPORT_SUGGESTED) else respondError(HttpStatusCode.TooManyRequests, FriendlyErrors.BUSY)
    }

    val prompt = Persona.systemPrompt(req.mood, gentle)
    val history = req.messages.takeLast(12)
    val started = System.currentTimeMillis()
    var reply = try {
        polishReply(provider.reply(prompt, history)).ifBlank { throw ProviderUnavailable("blank reply") }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        charged.release(uid)
        return when {
            gentle -> respondReply(fallback, SUPPORT_SUGGESTED)
            e is ProviderRateLimited -> respondError(HttpStatusCode.TooManyRequests, FriendlyErrors.BUSY)
            else -> respondError(HttpStatusCode.BadGateway, FriendlyErrors.NO_REPLY)
        }
    }
    if (soundsAlarming(reply)) {
        // A draft that brings up self-harm means the phrase list missed a heavy turn, so the rewrite gets the extra-care note.
        val rewritePrompt = if (!care && draftNamesSelfHarm(reply)) {
            care = true
            Persona.systemPrompt(req.mood, extraCare = true)
        } else prompt
        // One rewrite when time allows; either way the person gets a reply, never an error over a word.
        if (System.currentTimeMillis() - started < REWRITE_BUDGET_MILLIS && deps.globalLimiter.tryAcquire(GLOBAL_KEY)) {
            reply = try {
                polishReply(provider.reply(rewritePrompt + "\n\n" + Persona.rewriteNudge, history)).ifBlank { reply }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                reply
            }
        }
        // Whatever still slips through (or had no time for a rewrite) loses the sentences that carry those words.
        if (soundsAlarming(reply)) reply = withoutAlarmingSentences(reply) ?: if (care) fallback else Persona.neutralReply
    }
    respondReply(reply, if (care || mentionsProfessionalHelp(reply)) SUPPORT_SUGGESTED else NORMAL)
}

private suspend fun ApplicationCall.handleReport(deps: ChatDeps) {
    val verifyToken = deps.verifyToken ?: return respondError(HttpStatusCode.ServiceUnavailable, FriendlyErrors.UNAVAILABLE)
    val uid = authenticatedUid(verifyToken) ?: return

    val bodyText = receiveLimitedText(MAX_REPORT_CHARS) ?: return respondError(HttpStatusCode.PayloadTooLarge, FriendlyErrors.TOO_LONG)
    val req = try { json.decodeFromString(ReportRequest.serializer(), bodyText) } catch (_: Exception) { null }
    val replyText = req?.replyText?.trim().orEmpty()
    if (req == null || req.reason !in reportReasons || replyText.length !in 1..2000) {
        return respondError(HttpStatusCode.BadRequest, FriendlyErrors.INVALID)
    }
    if (!deps.reportLimiter.tryAcquire(uid)) return respondError(HttpStatusCode.TooManyRequests, FriendlyErrors.TOO_MANY_REPORTS)
    if (!deps.reportGlobalLimiter.tryAcquire(GLOBAL_KEY)) {
        deps.reportLimiter.release(uid)
        return respondError(HttpStatusCode.TooManyRequests, FriendlyErrors.TOO_MANY_REPORTS)
    }

    val report = StoredReport(Instant.now().toString(), req.reason, replyText, reporterId(uid))
    try {
        withContext(Dispatchers.IO) { deps.reportSink.save(report) }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        deps.reportLimiter.release(uid)
        deps.reportGlobalLimiter.release(GLOBAL_KEY)
        System.err.println("Could not store a reply report.")
        return respondError(HttpStatusCode.ServiceUnavailable, FriendlyErrors.UNAVAILABLE)
    }
    respond(HttpStatusCode.NoContent)
}
