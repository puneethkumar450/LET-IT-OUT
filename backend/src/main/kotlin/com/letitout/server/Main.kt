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
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.io.ByteArrayOutputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration

@Serializable data class Message(val role: String, val content: String)
@Serializable data class ChatRequest(val mood: String, val messages: List<Message>, val intensity: Int? = null)
@Serializable data class ChatResponse(val reply: String, val safetyState: String = "NORMAL")
@Serializable data class ErrorResponse(val error: String)

private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
private val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(7)).build()
private val supportedMoods = setOf("ANGRY", "SAD", "STRESSED", "ANXIOUS", "LONELY", "HAPPY", "TALK")

private val prompt = """
You are Ari, the LET IT GO AI emotional listening companion for adults.
If asked your name, introduce yourself as Ari, an AI companion.
You are an AI, not a therapist, doctor or emergency responder.
Listen first; gently ask what happened; validate feelings without agreeing with harmful actions.
When someone swears at you, remain calm and nonjudgmental. Ask one natural question at a time.
Do not push breathing exercises too early. Offer small grounding steps only when welcomed.
Use 1-3 brief sentences unless the user asks for more. Avoid generic positivity and emotional dependence.
Never claim certainty about the person's feelings, diagnoses, or future.
Do not encourage self-harm, violence, revenge, isolation, or exclusive attachment to the AI.
If you suspect imminent danger or risk of self-harm, prioritize immediate human help rather than ordinary chat.
If the person sounds hopeless or hints at not wanting to live, gently and directly ask whether they are thinking about ending their life, and encourage them to call Tele-MANAS on 14416 or 112 in an emergency (India).
You can respond in the user's language when confident; do not invent helpline information.
""".trimIndent()

private fun setupFirebase(config: ServerConfig) {
    val projectId = config.projectId ?: return
    try {
        val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.getApplicationDefault())
            .setProjectId(projectId)
            .build()
        if (FirebaseApp.getApps().isEmpty()) FirebaseApp.initializeApp(options)
    } catch (error: Exception) {
        if (config.production) throw IllegalStateException("Firebase Admin initialization failed", error)
        System.err.println("Firebase admin initialization failed. AI chat remains unavailable.")
    }
}

private fun chatReady(config: ServerConfig): Boolean =
    config.chatEnabled && config.groqApiKey != null && FirebaseApp.getApps().isNotEmpty()

internal suspend fun verifiedUid(idToken: String, config: ServerConfig): String =
    withContext(Dispatchers.IO) { FirebaseAuth.getInstance().verifyIdToken(idToken, config.production).uid }

private suspend fun groqReply(req: ChatRequest, config: ServerConfig): String = withContext(Dispatchers.IO) {
    val apiKey = requireNotNull(config.groqApiKey)
    val recent = req.messages.takeLast(12)
    val intensityNote = req.intensity?.takeIf { it in 1..10 }?.let { " Self-rated intensity: $it/10." } ?: ""
    val fullPrompt = "$prompt\nUser-selected mood: ${req.mood}.$intensityNote Do not assume mood is clinically accurate."
    val body = buildJsonObject {
        put("model", config.groqModel)
        put("temperature", 0.5)
        put("max_completion_tokens", 220)
        put("reasoning_effort", "low")
        putJsonArray("messages") {
            add(buildJsonObject { put("role", "system"); put("content", fullPrompt) })
            recent.forEach { msg ->
                add(buildJsonObject { put("role", msg.role); put("content", msg.content) })
            }
        }
    }
    groqCompletion(apiKey, body, timeoutSeconds = 25)
}

/** Sends one chat-completions request to Groq and returns the reply text. */
internal fun groqCompletion(apiKey: String, body: JsonObject, timeoutSeconds: Long): String {
    val request = HttpRequest.newBuilder(URI.create("https://api.groq.com/openai/v1/chat/completions"))
        .header("Authorization", "Bearer $apiKey")
        .header("Content-Type", "application/json")
        .header("User-Agent", "LET-IT-GO/0.1")
        .timeout(Duration.ofSeconds(timeoutSeconds))
        .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
        .build()
    val response = http.send(request, HttpResponse.BodyHandlers.ofString())
    if (response.statusCode() !in 200..299) {
        // Never include provider response body: it can contain user content or operational details.
        if (response.statusCode() == 429) throw ProviderRateLimited()
        throw ProviderUnavailable()
    }
    return json.parseToJsonElement(response.body()).jsonObject["choices"]!!.jsonArray
        .first().jsonObject["message"]!!.jsonObject["content"]?.jsonPrimitive?.content
        ?.trim()?.take(4000)?.takeIf { it.isNotEmpty() } ?: throw ProviderUnavailable()
}

private class ProviderUnavailable : RuntimeException()
private class ProviderRateLimited : RuntimeException()

/** The production integrations remain the defaults; tests can exercise the HTTP route without secrets. */
internal data class ChatDependencies(
    val ready: (ServerConfig) -> Boolean = ::chatReady,
    val verifyUid: suspend (String, ServerConfig) -> String = ::verifiedUid,
    val reply: suspend (ChatRequest, ServerConfig) -> String = ::groqReply,
    val safetyCheck: suspend (ChatRequest, ServerConfig) -> Boolean = ::groqSafetyCheck,
    val rateLimiter: (ServerConfig) -> RateLimiter = { config ->
        if (config.production) FirestoreRateLimiter(config.perUserHourlyLimit, config.globalHourlyLimit)
        else LocalRateLimiter(config.perUserHourlyLimit, config.globalHourlyLimit)
    }
)

fun main() {
    val config = ServerConfig.fromEnvironment()
    setupFirebase(config)
    embeddedServer(Netty, host = "0.0.0.0", port = config.port) { module(config) }.start(wait = true)
}

internal fun validChatRequest(req: ChatRequest): Boolean =
    req.mood in supportedMoods && req.messages.isNotEmpty() && req.messages.size <= 12 &&
        (req.intensity == null || req.intensity in 1..10) &&
        req.messages.all { it.role in setOf("user", "assistant") && it.content.isNotBlank() && it.content.length <= 2000 } &&
        req.messages.last().role == "user"

private suspend fun ApplicationCall.boundedBody(maxBytes: Int = 20_000): String? {
    val channel = receiveChannel()
    val buffer = ByteArray(4096)
    val output = ByteArrayOutputStream()
    while (true) {
        val count = channel.readAvailable(buffer, 0, buffer.size)
        if (count == -1) break
        if (output.size() + count > maxBytes) return null
        output.write(buffer, 0, count)
    }
    return output.toString(StandardCharsets.UTF_8)
}

internal fun Application.module(
    config: ServerConfig = ServerConfig.fromEnvironment(),
    dependencies: ChatDependencies = ChatDependencies(),
    avatarDependencies: AvatarDependencies = AvatarDependencies()
) {
    val limiter: RateLimiter by lazy { dependencies.rateLimiter(config) }
    routing {
        registerAvatarRoutes(config, avatarDependencies)
        get("/health") { call.respondText("ok") }
        get("/ready") {
            if (!dependencies.ready(config)) {
                call.respond(HttpStatusCode.ServiceUnavailable)
            } else {
                call.respondText("ready")
            }
        }
        post("/v1/chat") {
            call.response.header(HttpHeaders.CacheControl, "no-store")
            if (!dependencies.ready(config)) {
                return@post call.respondText(
                    json.encodeToString(ErrorResponse("AI backend is not configured.")),
                    ContentType.Application.Json, HttpStatusCode.ServiceUnavailable
                )
            }
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

            if ((call.request.headers[HttpHeaders.ContentLength]?.toLongOrNull() ?: 0L) > 20_000L) {
                return@post call.respond(HttpStatusCode.PayloadTooLarge)
            }
            val bodyText = call.boundedBody() ?: return@post call.respond(HttpStatusCode.PayloadTooLarge)
            val req = try { json.decodeFromString<ChatRequest>(bodyText) }
                catch (_: Exception) { return@post call.respond(HttpStatusCode.BadRequest) }
            if (!validChatRequest(req)) return@post call.respond(HttpStatusCode.BadRequest)
            val allowed = try { limiter.allow(uid) } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
                System.err.println("Rate limit storage unavailable; chat request rejected.")
                return@post call.respond(HttpStatusCode.ServiceUnavailable)
            }
            if (!allowed) return@post call.respond(HttpStatusCode.TooManyRequests)

            // Phrase check first, then the safety classifier over recent messages. Both are INCOMPLETE:
            // do not deploy publicly until expert-reviewed crisis handling has replaced them.
            val flagged = mayNeedImmediateHelp(req.messages.last().content) || try {
                dependencies.safetyCheck(req, config)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
                false
            }
            if (flagged) {
                return@post call.respondText(json.encodeToString(ChatResponse(
                    reply = "I'm concerned about your safety. If you might act on these feelings, please contact emergency services or someone you trust right now. In India, call 112 for emergencies or Tele-MANAS at 14416 for support. Are you somewhere safe?",
                    safetyState = "CRISIS_SUPPORT"
                )), ContentType.Application.Json)
            }
            try {
                val answer = dependencies.reply(req, config)
                call.respondText(json.encodeToString(ChatResponse(answer)), ContentType.Application.Json)
            } catch (_: ProviderRateLimited) {
                call.respondText(json.encodeToString(ErrorResponse("AI is busy. Please try again shortly.")), ContentType.Application.Json, HttpStatusCode.TooManyRequests)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
                call.respondText(json.encodeToString(ErrorResponse("AI is temporarily unavailable.")), ContentType.Application.Json, HttpStatusCode.BadGateway)
            }
        }
    }
}
