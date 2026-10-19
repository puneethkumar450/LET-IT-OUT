package com.letitout.app.data

import androidx.compose.ui.graphics.Color

enum class Mood(val label: String, val prompt: String, val heading: String, val reassurance: String) {
    ANGRY("Angry", "What happened today? I'm listening.", "Feeling angry", "It's okay to feel this way. You can say it however it comes out — no judgment here."),
    SAD("Sad", "I'm here. Whenever you're ready, what's been on your mind?", "Feeling sad", "You don't have to explain it perfectly. Take all the time you need."),
    STRESSED("Stressed", "What's feeling overwhelming right now?", "Feeling stressed", "Let's slow things down. We can take it one thing at a time."),
    ANXIOUS("Anxious", "What's on your mind? We can take it one thing at a time.", "Feeling anxious", "You can say what's worrying you here. We'll go at your pace."),
    LONELY("Lonely", "I'm here to listen. What would you like to talk about?", "Feeling lonely", "I'm glad you reached out. You can talk about anything here."),
    HAPPY("Happy", "I'd love to hear what's going well!", "Feeling happy", "That's lovely. Tell me what's making today feel good."),
    TALK("Just talk", "Hey, what's going on?", "Just want to talk", "No label needed — start anywhere.")
}

/** surface = screen background, bubble = user bubble and chips, accent = buttons and icons, tile = Home card. */
data class MoodPalette(val surface: Color, val bubble: Color, val accent: Color, val tile: Color)

fun Mood.palette() = when (this) {
    Mood.ANGRY -> MoodPalette(Color(0xFFFFF3EC), Color(0xFFFFD9C9), Color(0xFFC9633F), Color(0xFFFFE3D6))
    Mood.SAD -> MoodPalette(Color(0xFFEEF4FF), Color(0xFFDCE7FF), Color(0xFF3F7FC9), Color(0xFFDDEBFF))
    Mood.STRESSED -> MoodPalette(Color(0xFFECFBF6), Color(0xFFCFF2E6), Color(0xFF1F8C72), Color(0xFFD9F5EA))
    Mood.ANXIOUS -> MoodPalette(Color(0xFFEDF8FA), Color(0xFFD4EEF3), Color(0xFF2A8BA3), Color(0xFFD8F2F6))
    Mood.LONELY -> MoodPalette(Color(0xFFFFF0F5), Color(0xFFFFDCE8), Color(0xFFC9507F), Color(0xFFFFE0EC))
    Mood.HAPPY -> MoodPalette(Color(0xFFFFFBE8), Color(0xFFFFF0B8), Color(0xFFA67C00), Color(0xFFFFF3BE))
    Mood.TALK -> MoodPalette(Color(0xFFF3F0FF), Color(0xFFE6E1FF), Color(0xFF6A5AE0), Color(0xFFE6E1FF))
}

/** [supportResources] marks a reply the backend flagged for crisis support, shown with helpline actions. */
data class ChatMessage(val id: Long, val text: String, val fromUser: Boolean, val supportResources: Boolean = false)

data class ApiMessage(val role: String, val content: String)
data class ChatRequest(val mood: String, val messages: List<ApiMessage>, val intensity: Int? = null)
data class ChatResponse(val reply: String, val safetyState: String = "NORMAL")
