package com.letitout.server

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.future.await
import kotlinx.serialization.json.*
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

interface ReplyProvider {
    suspend fun reply(systemPrompt: String, messages: List<Message>): String
}

class ProviderUnavailable(message: String? = null) : RuntimeException(message)
class ProviderRateLimited : RuntimeException()

class GroqProvider(
    private val apiKey: String,
    private val model: String = DEFAULT_MODEL,
    private val maxCompletionTokens: Int = DEFAULT_MAX_COMPLETION_TOKENS,
    private val reasoningEffort: String = DEFAULT_REASONING_EFFORT,
    private val timeout: Duration = Duration.ofSeconds(30)
) : ReplyProvider {
    private val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(7)).build()

    override suspend fun reply(systemPrompt: String, messages: List<Message>): String {
        val request = HttpRequest.newBuilder(URI.create("https://api.groq.com/openai/v1/chat/completions"))
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .timeout(timeout)
            .POST(HttpRequest.BodyPublishers.ofString(requestBody(systemPrompt, messages)))
            .build()
        // Async, so a waiting call holds no thread and a cancelled request (client gone) aborts the exchange.
        val pending = http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
        val response = try { pending.await() }
            catch (e: CancellationException) { pending.cancel(true); throw e }
            catch (e: java.io.IOException) { throw ProviderUnavailable(e.javaClass.simpleName) }
        // Never log or forward the provider body: it can contain user content or operational details.
        return when (val status = response.statusCode()) {
            in 200..299 -> {
                var hitLimit = false
                val content = parseGroqContent(response.body()) { hitLimit = true }
                if (hitLimit) System.err.println("Groq reply hit the token limit. Consider raising GROQ_MAX_COMPLETION_TOKENS.")
                content ?: run {
                    if (!hitLimit) System.err.println("Groq returned no visible reply. If this repeats, raise GROQ_MAX_COMPLETION_TOKENS.")
                    throw ProviderUnavailable(if (hitLimit) "length" else "empty content")
                }
            }
            429 -> throw ProviderRateLimited()
            else -> {
                System.err.println("Groq request failed with HTTP $status.")
                throw ProviderUnavailable("HTTP $status")
            }
        }
    }

    internal fun requestBody(systemPrompt: String, messages: List<Message>): String = buildJsonObject {
        put("model", model)
        put("temperature", 0.8)
        put("max_completion_tokens", maxCompletionTokens)
        // gpt-oss spends part of the completion budget on hidden reasoning; other Groq models reject this field.
        if (model.startsWith("openai/gpt-oss")) put("reasoning_effort", reasoningEffort)
        putJsonArray("messages") {
            add(buildJsonObject { put("role", "system"); put("content", systemPrompt) })
            messages.forEach { msg -> add(buildJsonObject { put("role", msg.role); put("content", msg.content) }) }
        }
    }.toString()

    companion object {
        const val DEFAULT_MODEL = "openai/gpt-oss-120b"
        const val DEFAULT_MAX_COMPLETION_TOKENS = 2048
        const val DEFAULT_REASONING_EFFORT = "medium"

        fun fromEnvironment(env: (String) -> String? = System::getenv): GroqProvider? {
            val apiKey = env("GROQ_API_KEY")?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            return GroqProvider(
                apiKey = apiKey,
                model = env("GROQ_MODEL")?.trim()?.takeIf { it.isNotEmpty() } ?: DEFAULT_MODEL,
                maxCompletionTokens = env("GROQ_MAX_COMPLETION_TOKENS")?.trim()?.toIntOrNull()
                    ?.takeIf { it in 64..65_536 } ?: DEFAULT_MAX_COMPLETION_TOKENS,
                reasoningEffort = env("GROQ_REASONING_EFFORT")?.trim()?.lowercase()
                    ?.takeIf { it in setOf("low", "medium", "high") } ?: DEFAULT_REASONING_EFFORT
            )
        }
    }
}

private val groqJson = Json { ignoreUnknownKeys = true }

/**
 * Returns the visible reply text, ignoring any `reasoning` field; null when content is missing or blank. When the token
 * limit cut the reply off (`finish_reason` "length"), calls [onTruncated] and keeps only the complete sentences (null if none).
 */
internal fun parseGroqContent(body: String, onTruncated: () -> Unit = {}): String? = runCatching {
    val choice = groqJson.parseToJsonElement(body).jsonObject["choices"]?.jsonArray?.firstOrNull()?.jsonObject ?: return null
    val content = (choice["message"]?.jsonObject?.get("content") as? JsonPrimitive)?.takeIf { it.isString }?.content?.trim()
    val truncated = (choice["finish_reason"] as? JsonPrimitive)?.contentOrNull == "length"
    if (truncated) onTruncated()
    (if (truncated) content?.let(::completeSentences) else content)?.takeIf { it.isNotEmpty() }
}.getOrNull()

private fun completeSentences(text: String): String {
    var end = text.indexOfLast { it == '.' || it == '!' || it == '?' || it == '\u0964' } + 1
    while (end in 1 until text.length && text[end] in "\"\u201D\u2019')") end++
    return text.take(end).trim()
}

private const val MAX_REPLY_CHARS = 1_800

internal fun polishReply(text: String): String {
    var result = text.replace("\r\n", "\n").trim()
    while (result.length >= 2 && wrappedInQuotes(result)) result = result.substring(1, result.length - 1).trim()
    result = result.replace("**", "")
        .lines().joinToString("\n") { it.trimEnd().replace(Regex("^\\s*(?:[-*\u2022]|#{1,6}|\\d{1,2}[.)])\\s+"), "") }
        .replace(Regex("(?<![\\w*])\\*(?=\\S)([^*\\n]+?)(?<=\\S)\\*(?![\\w*])"), "$1")
        .replace("__", "").replace("`", "")
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()
    if (result.length <= MAX_REPLY_CHARS) return result
    val cut = result.take(MAX_REPLY_CHARS)
    val sentenceEnd = cut.indexOfLast { it == '.' || it == '!' || it == '?' || it == '\u0964' }
    return if (sentenceEnd >= MAX_REPLY_CHARS / 3) cut.take(sentenceEnd + 1).trim()
    else cut.substringBeforeLast(' ').trimEnd() + "\u2026"
}

private fun wrappedInQuotes(text: String): Boolean {
    val inner = text.substring(1, text.length - 1)
    if (text.first() == '"' && text.last() == '"') return '"' !in inner
    if (text.first() != '\u201C' || text.last() != '\u201D') return false
    var depth = 0
    for (c in inner) {
        if (c == '\u201C') depth++
        if (c == '\u201D' && --depth < 0) return false
    }
    return depth == 0
}
