package com.letitout.app.data

import androidx.compose.ui.graphics.Color

enum class Mood(val label: String, val prompt: String, val heading: String, val reassurance: String) {
    ANGRY("Angry", "Sounds like something really got to you. What happened? Say it however it comes out.", "Feeling angry", "It's okay to feel this way. You can say it however it comes out — no judgment here."),
    SAD("Sad", "Hey, I'm glad you came. What's been weighing on you? Start wherever feels easiest.", "Feeling sad", "You don't have to explain it perfectly. Take all the time you need."),
    STRESSED("Stressed", "That sounds like a lot to carry. What's taking up the most space in your head right now?", "Feeling stressed", "Let's slow things down. We can take it one thing at a time."),
    ANXIOUS("Anxious", "I'm right here with you. What's got your mind racing? We can untangle it slowly.", "Feeling anxious", "You can say what's worrying you here. We'll go at your pace."),
    LONELY("Lonely", "Really glad you're here. How has your day actually been?", "Feeling lonely", "I'm glad you reached out. You can talk about anything here."),
    HAPPY("Happy", "Ooh, I like this already. Tell me everything, what's the good news?", "Feeling happy", "That's lovely. Tell me what's making today feel good."),
    TALK("Just talk", "Hey, good to see you. What's on your mind today?", "Just want to talk", "No label needed — start anywhere.")
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

/** [supportResources] marks a reply that sounded heavy; the chat shows it with a gentle note and the Tele-MANAS line. */
data class ChatMessage(val id: Long, val text: String, val fromUser: Boolean, val supportResources: Boolean = false)

data class AppSettings(
    val readAloud: Boolean = true,
    val voiceInput: Boolean = true,
    val followMoodTheme: Boolean = true,
    val onboarded: Boolean = false
)

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
data class ChatRequest(val mood: String, val messages: List<ApiMessage>, val intensity: Int? = null)
data class ChatResponse(val reply: String, val safetyState: String = "NORMAL")
data class ErrorBody(val error: String? = null)

/**
 * The body for `POST /v1/chat`: the opening question the app showed goes first, so the reply follows on from what they
 * were asked; each message is trimmed to 2,000 characters, blanks are dropped, and only the last 12 are sent.
 * [intensity] is the self-rated 1–10 from the mood check-in; anything outside that range is left out.
 */
internal fun chatRequestFor(mood: Mood, conversation: List<ChatMessage>, intensity: Int? = null): ChatRequest = ChatRequest(
    mood.name,
    (listOf(ApiMessage("assistant", mood.prompt)) + conversation
        .map { it.copy(text = it.text.trim().take(2000)) }
        .filter { it.text.isNotBlank() }
        .map { ApiMessage(if (it.fromUser) "user" else "assistant", it.text) })
        .takeLast(12),
    intensity?.takeIf { it in 1..10 }
)

internal fun ChatResponse.toChatReply(): ChatReply {
    val text = reply.trim()
    if (text.isEmpty()) throw FriendlyException("I couldn't put a reply together just now. Mind trying again?")
    return ChatReply(text, safetyState == "SUPPORT_SUGGESTED")
}

/** Body of `POST /v1/report`: why the person flagged a reply, and that reply (at most 2,000 chars). */
data class ReportRequest(val reason: String, val replyText: String)
