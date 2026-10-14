package com.letitout.app.data

import androidx.compose.ui.graphics.Color

enum class Mood(val label: String, val detail: String, val prompt: String) {
    ANGRY("Angry", "Let it out", "What happened today? I'm listening."),
    SAD("Sad", "Take your time", "Would you like to tell me what's weighing on you?"),
    STRESSED("Stressed", "One thing at a time", "What's feeling overwhelming right now?"),
    ANXIOUS("Anxious", "Find some ground", "What's on your mind? We can take it one thing at a time."),
    LONELY("Lonely", "You can talk here", "I'm here to listen. What would you like to talk about?"),
    HAPPY("Happy", "Share the good", "I'd love to hear what's going well!"),
    TALK("Just talk", "No label needed", "Hey, what's going on?")
}

data class MoodPalette(val background: Color, val primary: Color, val accent: Color)

fun Mood.palette() = when (this) {
    Mood.ANGRY -> MoodPalette(Color(0xFFF9EEE9), Color(0xFFA45B46), Color(0xFFECC5B7))
    Mood.SAD -> MoodPalette(Color(0xFFECF2F6), Color(0xFF4E7088), Color(0xFFC8DCE8))
    Mood.STRESSED -> MoodPalette(Color(0xFFEDF4EE), Color(0xFF557B60), Color(0xFFD2E4D4))
    Mood.ANXIOUS -> MoodPalette(Color(0xFFF2F0F7), Color(0xFF76688F), Color(0xFFDDD6E8))
    Mood.LONELY -> MoodPalette(Color(0xFFF7EFF1), Color(0xFF916B79), Color(0xFFE8D5DB))
    Mood.HAPPY -> MoodPalette(Color(0xFFF9F3E7), Color(0xFF98743B), Color(0xFFECDCAF))
    Mood.TALK -> MoodPalette(Color(0xFFEBF4F0), Color(0xFF366E63), Color(0xFFCBE2D8))
}

data class ChatMessage(val id: Long, val text: String, val fromUser: Boolean)

data class ApiMessage(val role: String, val content: String)
data class ChatRequest(val mood: String, val messages: List<ApiMessage>)
data class ChatResponse(val reply: String, val safetyState: String = "NORMAL")
