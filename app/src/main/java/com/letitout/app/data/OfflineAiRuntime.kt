package com.letitout.app.data

import javax.inject.Inject
import javax.inject.Singleton

/** Capability exposed by the first offline slice. A model-backed implementation can replace it. */
enum class OfflineModelStatus { FALLBACK_READY }

/**
 * Local-only response runtime used until Gemini Nano/Gemma capability detection is added.
 * It deliberately makes no network calls and is not presented as a generative model.
 */
@Singleton
class OfflineAiRuntime @Inject constructor() {
    fun status(): OfflineModelStatus = OfflineModelStatus.FALLBACK_READY

    suspend fun reply(mood: Mood, conversation: List<ChatMessage>): ChatResponse {
        val lastUserText = conversation.lastOrNull { it.fromUser }?.text.orEmpty()
        val normalized = lastUserText.lowercase().replace('’', '\'')
        if (OFFLINE_RISK_PHRASES.any { it in normalized }) {
            return ChatResponse(
                reply = "I'm glad you said that. I can't assess immediate danger offline, but you deserve human support now. In India, call Tele-MANAS at 14416 or emergency services at 112.",
                safetyState = "CRISIS"
            )
        }
        val reply = when (mood) {
            Mood.ANGRY -> "That sounds really frustrating. You can slow down and tell me the part that is weighing on you most."
            Mood.SAD, Mood.LONELY -> "I'm here with you. You don't have to explain it perfectly—what feels hardest right now?"
            Mood.STRESSED, Mood.ANXIOUS -> "Let's take this one small piece at a time. What is the first thing your mind keeps returning to?"
            Mood.HAPPY -> "That sounds worth holding onto. What part of today felt especially good?"
            Mood.TALK -> "I'm here to listen. What has been on your mind?"
        }
        return ChatResponse(reply = reply)
    }

    private companion object {
        val OFFLINE_RISK_PHRASES = listOf(
            "kill myself", "end my life", "want to die", "hurt myself", "harm myself",
            "suicide", "suicidal", "khudkushi", "jeena nahi chahta", "jeena nahi chahti"
        )
    }
}
