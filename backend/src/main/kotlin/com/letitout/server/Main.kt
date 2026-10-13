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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@Serializable data class Message(val role: String, val content: String)
@Serializable data class ChatRequest(val mood: String, val messages: List<Message>)
@Serializable data class ChatResponse(val reply: String, val safetyState: String = "NORMAL")
@Serializable data class ErrorResponse(val error: String)

private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
private val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(7)).build()
private val supportedMoods = setOf("ANGRY", "SAD", "STRESSED", "ANXIOUS", "LONELY", "HAPPY", "TALK")

// Prototype guardrails only. This does not reliably detect or triage all crises.
internal fun mayNeedImmediateHelp(text: String): Boolean {
    val content = text.lowercase()
    val signals = listOf(
        "kill myself", "end my life", "want to die", "going to suicide",
        "hurt myself", "i will kill", "going to kill someone",
        "i'm about to hurt", "i want to kill myself", "i am going to hurt"
    )
    return signals.any { it in content }
}

private val prompt = """
You are LET IT OUT, an AI emotional listening companion for adults.
You are an AI, not a therapist, doctor or emergency responder.
Listen first; gently ask what happened; validate feelings without agreeing with harmful actions.
When someone swears at you, remain calm and nonjudgmental. Ask one natural question at a time.
Do not push breathing exercises too early. Offer small grounding steps only when welcomed.
Use 1-3 brief sentences unless the user asks for more. Avoid generic positivity and emotional dependence.
Never claim certainty about the person's feelings, diagnoses, or future.
Do not encourage self-harm, violence, revenge, isolation, or exclusive attachment to the AI.
If you suspect imminent danger or risk of self-harm, prioritize immediate human help rather than ordinary chat.
You can respond in the user's language when confident; do not invent helpline information.
""".trimIndent()

private val requestTimestamps = ConcurrentHashMap<String, MutableList<Long>>()
private fun withinLimit(uid: String): Boolean {
    val now = System.currentTimeMillis()
    val times = requestTimestamps.computeIfAbsent(uid) { mutableListOf() }
    synchronized(times) {
        times.removeAll { now - it > 60 * 60 * 1000L }
        if (times.size >= 20) return false
        times.add(now)
        return true
    }
}

private fun setupFirebase() {
    val projectId = System.getenv("FIREBASE_PROJECT_ID")
    if (projectId.isNullOrBlank()) return
    runCatching {
        val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.getApplicationDefault())
            .setProjectId(projectId)
            .build()
        if (FirebaseApp.getApps().isEmpty()) FirebaseApp.initializeApp(options)
    }.onFailure { System.err.println("Firebase admin initialization failed. AI chat remains unavailable.") }
}

private fun providerConfigured(): Boolean = System.getenv("GROQ_API_KEY")?.isNotBlank() == true

private suspend fun groqReply(req: ChatRequest): String = withContext(Dispatchers.IO) {
    val apiKey = requireNotNull(System.getenv("GROQ_API_KEY"))
    val model = System.getenv("GROQ_MODEL") ?: "openai/gpt-oss-120b"
    val recent = req.messages.takeLast(12)
    val fullPrompt = "$prompt\nUser-selected mood: ${req.mood}. Do not assume mood is clinically accurate."
    val body = buildJsonObject {
        put("model", model)
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
    val request = HttpRequest.newBuilder(URI.create("https://api.groq.com/openai/v1/chat/completions"))
        .header("Authorization", "Bearer $apiKey")
        .header("Content-Type", "application/json")
        .timeout(Duration.ofSeconds(25))
        .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
        .build()
    val response = http.send(request, HttpResponse.BodyHandlers.ofString())
    if (response.statusCode() !in 200..299) {
        // Never include provider response body: it can contain user content or operational details.
        if (response.statusCode() == 429) throw ProviderRateLimited()
        throw ProviderUnavailable()
    }
    json.parseToJsonElement(response.body()).jsonObject["choices"]!!.jsonArray
        .first().jsonObject["message"]!!.jsonObject["content"]?.jsonPrimitive?.content
        ?.trim()?.takeIf { it.isNotEmpty() } ?: throw ProviderUnavailable()
}

private class ProviderUnavailable : RuntimeException()
private class ProviderRateLimited : RuntimeException()

fun main() {
    setupFirebase()
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, host = "0.0.0.0", port = port) { module() }.start(wait = true)
}

fun Application.module() {
    routing {
        get("/health") { call.respondText("ok") }
        post("/v1/chat") {
            if (FirebaseApp.getApps().isEmpty() || !providerConfigured()) {
                return@post call.respondText(
                    json.encodeToString(ErrorResponse("AI backend is not configured.")),
                    ContentType.Application.Json, HttpStatusCode.ServiceUnavailable
                )
            }
            val idToken = call.request.headers[HttpHeaders.Authorization]
                ?.takeIf { it.startsWith("Bearer ") }?.removePrefix("Bearer ")
            if (idToken.isNullOrBlank()) return@post call.respond(HttpStatusCode.Unauthorized)
            val uid = try {
                withContext(Dispatchers.IO) { FirebaseAuth.getInstance().verifyIdToken(idToken).uid }
            } catch (_: Exception) { return@post call.respond(HttpStatusCode.Unauthorized) }

            val bodyText = call.receiveText()
            if (bodyText.length > 20_000) return@post call.respond(HttpStatusCode.PayloadTooLarge)
            val req = try { json.decodeFromString<ChatRequest>(bodyText) }
                catch (_: Exception) { return@post call.respond(HttpStatusCode.BadRequest) }
            if (req.mood !in supportedMoods || req.messages.isEmpty() || req.messages.size > 12 ||
                req.messages.any { it.role !in setOf("user", "assistant") || it.content.length !in 1..2000 } ||
                req.messages.last().role != "user"
            ) return@post call.respond(HttpStatusCode.BadRequest)
            if (!withinLimit(uid)) return@post call.respond(HttpStatusCode.TooManyRequests)

            val latestText = req.messages.last().content
            // This emergency keyword check is deliberately conservative and INCOMPLETE.
            // Do not deploy publicly until expert-reviewed crisis handling has replaced it.
            if (mayNeedImmediateHelp(latestText)) {
                return@post call.respondText(json.encodeToString(ChatResponse(
                    reply = "I'm concerned about your safety. If you might act on these feelings, please contact emergency services or someone you trust right now. In India, call 112 for emergencies or Tele-MANAS at 14416 for support. Are you somewhere safe?",
                    safetyState = "CRISIS_SUPPORT"
                )), ContentType.Application.Json)
            }
            try {
                val answer = groqReply(req)
                call.respondText(json.encodeToString(ChatResponse(answer)), ContentType.Application.Json)
            } catch (_: ProviderRateLimited) {
                call.respondText(json.encodeToString(ErrorResponse("AI is busy. Please try again shortly.")), ContentType.Application.Json, HttpStatusCode.TooManyRequests)
            } catch (_: Exception) {
                call.respondText(json.encodeToString(ErrorResponse("AI is temporarily unavailable.")), ContentType.Application.Json, HttpStatusCode.BadGateway)
            }
        }
    }
}
