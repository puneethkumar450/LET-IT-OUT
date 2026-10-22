package com.letitout.app.data

import androidx.compose.ui.graphics.Color

enum class Mood(val label: String, val detail: String, val prompt: String) {
    ANGRY("Angry", "Let it out", "Sounds like something really got to you. What happened? Say it however it comes out."),
    SAD("Sad", "Take your time", "Hey, I'm glad you came. What's been weighing on you? Start wherever feels easiest."),
    STRESSED("Stressed", "One thing at a time", "That sounds like a lot to carry. What's taking up the most space in your head right now?"),
    ANXIOUS("Anxious", "Find some ground", "I'm right here with you. What's got your mind racing? We can untangle it slowly."),
    LONELY("Lonely", "You can talk here", "Really glad you're here. How has your day actually been?"),
    HAPPY("Happy", "Share the good", "Ooh, I like this already. Tell me everything, what's the good news?"),
    TALK("Just talk", "No label needed", "Hey, good to see you. What's on your mind today?")
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

enum class Feeling { LIGHTER, SAME, HEAVIER }

/** Summary of the last 10 "How are you feeling now?" answers, kept on the phone only. */
data class CheckInStats(val total: Int = 0, val lighter: Int = 0)

data class ReminderSettings(val enabled: Boolean = false, val hour: Int = 20, val minute: Int = 0)

data class ChatReply(val text: String, val supportSuggested: Boolean)

/** Why someone flagged a reply. [slug] is what the backend receives. */
enum class ReportReason(val label: String, val slug: String) {
    HURTFUL("Hurtful or offensive", "hurtful"),
    UNSAFE("Advice that could cause harm", "unsafe"),
    WRONG("Wrong or made up", "wrong"),
    OTHER("Something else", "other")
}

data class ApiMessage(val role: String, val content: String)
data class ChatRequest(val mood: String, val messages: List<ApiMessage>)
data class ChatResponse(val reply: String, val safetyState: String = "NORMAL")
data class ErrorBody(val error: String? = null)

/** Body of `POST /v1/report`: why the person flagged a reply, and that reply (at most 2,000 chars). */
data class ReportRequest(val reason: String, val replyText: String)
