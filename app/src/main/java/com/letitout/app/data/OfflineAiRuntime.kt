package com.letitout.app.data

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val GEMMA_MODEL_NAME = "ari-gemma.task"

/** Capability exposed by the first offline slice. A model-backed implementation can replace it. */
enum class OfflineModelStatus { FALLBACK_READY, GEMMA_READY }

/**
 * Local-only response runtime used until Gemini Nano/Gemma capability detection is added.
 * It deliberately makes no network calls and is not presented as a generative model.
 */
@Singleton
class OfflineAiRuntime private constructor(
    private val modelFile: File,
    private val appContext: Context?
) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(
        File(File(context.filesDir, "offline-models"), GEMMA_MODEL_NAME),
        context
    )

    internal constructor(modelDirectory: File) : this(File(modelDirectory, GEMMA_MODEL_NAME), null)

    fun status(): OfflineModelStatus = if (modelFile.isFile) OfflineModelStatus.GEMMA_READY else OfflineModelStatus.FALLBACK_READY

    suspend fun reply(mood: Mood, conversation: List<ChatMessage>): ChatResponse {
        val lastUserText = conversation.lastOrNull { it.fromUser }?.text.orEmpty()
        val normalized = lastUserText.lowercase().replace('’', '\'')
        if (OFFLINE_RISK_PHRASES.any { it in normalized }) {
            return ChatResponse(
                reply = "I'm glad you said that. I can't assess immediate danger offline, but you deserve human support now. In India, call Tele-MANAS at 14416 or emergency services at 112.",
                safetyState = "CRISIS"
            )
        }
        gemmaReplyOrNull(mood, conversation)?.let { return ChatResponse(reply = it) }
        val reply = when (mood) {
            Mood.ANGRY -> "That sounds really frustrating. You can slow down and tell me the part that is weighing on you most."
            Mood.SAD, Mood.LONELY -> "I'm here with you. You don't have to explain it perfectly—what feels hardest right now?"
            Mood.STRESSED, Mood.ANXIOUS -> "Let's take this one small piece at a time. What is the first thing your mind keeps returning to?"
            Mood.HAPPY -> "That sounds worth holding onto. What part of today felt especially good?"
            Mood.TALK -> "I'm here to listen. What has been on your mind?"
        }
        return ChatResponse(reply = reply)
    }

    private suspend fun gemmaReplyOrNull(mood: Mood, conversation: List<ChatMessage>): String? {
        if (!modelFile.isFile || appContext == null) return null
        val prompt = buildString {
            append("You are Ari, a warm listening companion for adults. ")
            append("You are not a therapist or emergency service. Keep the reply under 45 words.\n")
            append("Mood: ").append(mood.label).append('\n')
            conversation.takeLast(8).forEach { message ->
                append(if (message.fromUser) "User: " else "Ari: ").append(message.text).append('\n')
            }
            append("Ari:")
        }
        return runCatching {
            withContext(Dispatchers.Default) {
                val options = LlmInferenceOptions.builder()
                    .setModelPath(modelFile.absolutePath)
                    .setMaxTokens(256)
                    .build()
                LlmInference.createFromOptions(appContext, options).use { inference ->
                    inference.generateResponse(prompt).trim().takeIf { it.isNotBlank() }
                }
            }
        }.getOrNull()
    }

    private companion object {
        val OFFLINE_RISK_PHRASES = listOf(
            "kill myself", "end my life", "want to die", "hurt myself", "harm myself",
            "suicide", "suicidal", "khudkushi", "jeena nahi chahta", "jeena nahi chahti"
        )
    }
}
