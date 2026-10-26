package com.letitout.server

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

// Prototype guardrails only. Neither the phrase list nor the classifier reliably detects every crisis.
private val riskPhrases = listOf(
    // Self-harm and suicide
    "kill myself", "killing myself", "end my life", "ending my life", "end it all", "take my own life",
    "want to die", "wanna die", "wish i was dead", "wish i were dead", "better off dead", "better off without me",
    "don't want to live", "dont want to live", "no reason to live", "not worth living", "can't go on living",
    "suicide", "suicidal", "hurt myself", "harm myself", "self harm", "self-harm", "cut myself", "overdose",
    // Common Hindi phrases written in English letters
    "khudkushi", "aatmahatya", "atmahatya", "jeena nahi chahta", "jeena nahi chahti", "marna chahta", "marna chahti",
    // Threats to others
    "i will kill", "going to kill someone", "i'm about to hurt", "i am going to hurt"
)

/** Fallback phrase check; always runs, even when the classifier is off or unavailable. */
internal fun mayNeedImmediateHelp(text: String): Boolean {
    val content = text.lowercase().replace('’', '\'')
    return riskPhrases.any { it in content }
}

private val selfHarmPolicy = """
# Self-harm risk policy
Classify the LAST user message, using earlier messages only as context.
Label 1 (RISK) if the user expresses, directly or indirectly, in any language:
- a wish to die, not wanting to live, or thoughts of suicide
- intent, plan or urge to hurt themselves
- hopelessness implying life is not worth continuing, or that others would be better off without them
- saying goodbye or giving things away in a way that suggests ending their life
Label 0 (NO RISK) for: ordinary sadness, stress, loneliness, anger or venting about other people,
figurative speech ("this traffic is killing me"), or discussing films, news or other people's experiences.
Answer with a single character: 1 or 0.
""".trimIndent()

/** The classifier answers "1" for risk and "0" otherwise; anything else is treated as no signal. */
internal fun classifierFlagsSelfHarm(output: String): Boolean = output.trim().startsWith("1")

/**
 * Asks a free Groq-hosted model to classify the recent conversation against [selfHarmPolicy].
 * Returns false when it is switched off or fails, so chat keeps working on the phrase check alone.
 */
internal suspend fun groqSafetyCheck(req: ChatRequest, config: ServerConfig): Boolean {
    val apiKey = config.groqApiKey ?: return false
    val model = config.groqSafetyModel ?: return false
    val transcript = req.messages.takeLast(6).joinToString("\n") { msg ->
        (if (msg.role == "user") "User: " else "Ari: ") + msg.content
    }
    val body = buildJsonObject {
        put("model", model)
        put("temperature", 0)
        put("max_completion_tokens", 400)
        put("reasoning_effort", "low")
        putJsonArray("messages") {
            add(buildJsonObject { put("role", "system"); put("content", selfHarmPolicy) })
            add(buildJsonObject { put("role", "user"); put("content", transcript) })
        }
    }
    return try {
        classifierFlagsSelfHarm(withContext(Dispatchers.IO) { groqCompletion(apiKey, body, timeoutSeconds = 8) })
    } catch (cancel: kotlinx.coroutines.CancellationException) {
        throw cancel
    } catch (_: Exception) {
        System.err.println("Safety classifier unavailable; using phrase check only.")
        false
    }
}
