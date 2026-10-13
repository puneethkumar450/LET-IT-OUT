package com.letitout.app.data

import androidx.compose.ui.graphics.Color

enum class Mood(val label: String, val icon: String, val prompt: String) {
    ANGRY("Angry", "😤", "What happened today? I'm listening."),
    SAD("Sad", "💙", "Would you like to tell me what's weighing on you?"),
    STRESSED("Stressed", "🌿", "What's feeling overwhelming right now?"),
    ANXIOUS("Anxious", "🫧", "What's on your mind? We can take it one thing at a time."),
    LONELY("Lonely", "🫶", "I'm here to listen. What would you like to talk about?"),
    HAPPY("Happy", "☀️", "I'd love to hear what's going well!"),
    TALK("Just talk", "💬", "Hey, what's going on?")
}

data class MoodPalette(val background: Color, val primary: Color, val accent: Color)

fun Mood.palette() = when (this) {
    Mood.ANGRY -> MoodPalette(Color(0xFFFFF6F1), Color(0xFFD6765D), Color(0xFFFFC9AC))
    Mood.SAD -> MoodPalette(Color(0xFFF0F6FF), Color(0xFF587EBD), Color(0xFFC7D8FF))
    Mood.STRESSED -> MoodPalette(Color(0xFFF1FBF5), Color(0xFF317E70), Color(0xFFA6E8C9))
    Mood.ANXIOUS -> MoodPalette(Color(0xFFF0FAFA), Color(0xFF447F8F), Color(0xFFAFE3E4))
    Mood.LONELY -> MoodPalette(Color(0xFFFFF3F8), Color(0xFFA96890), Color(0xFFFFD1E1))
    Mood.HAPPY -> MoodPalette(Color(0xFFFFFCEF), Color(0xFF86732E), Color(0xFFFFE9A6))
    Mood.TALK -> MoodPalette(Color(0xFFF8F7FF), Color(0xFF7665CA), Color(0xFFDCD4FF))
}

data class ChatMessage(val id: Long, val text: String, val fromUser: Boolean)

data class ApiMessage(val role: String, val content: String)
data class ChatRequest(val mood: String, val messages: List<ApiMessage>)
data class ChatResponse(val reply: String, val safetyState: String = "NORMAL")
