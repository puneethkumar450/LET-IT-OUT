package com.letitout.app.data

import android.app.Activity
import kotlinx.coroutines.flow.Flow

interface AuthGateway {
    fun isConfigured(): Boolean
    fun isGoogleConfigured(): Boolean
    fun isSignedIn(): Boolean
    /** True for an anonymous guest, who can upgrade to Google without losing anything on this phone. */
    fun isGuest(): Boolean
    fun userName(): String?
    fun userEmail(): String?
    /** Throws [FriendlyException]. */
    suspend fun signInGuest()
    /** Returns true when signed in, false when the person closed the account picker. Throws [FriendlyException]. */
    suspend fun signInGoogle(activity: Activity): Boolean
    suspend fun signOut()
    /** Throws [FriendlyException], for example when Firebase needs a recent sign-in. */
    suspend fun deleteAccount()
}

interface ChatGateway {
    /** [intensity] is the self-rated 1–10 from the mood check-in, or null when none was given. */
    suspend fun reply(mood: Mood, conversation: List<ChatMessage>, intensity: Int?): ChatReply
    /** Sends one reply the person chose to flag, and why. Throws on failure. */
    suspend fun report(reason: ReportReason, replyText: String)
}

/** Everything here stays on the phone. */
interface UserPreferences {
    val mood: Flow<Mood>
    suspend fun setMood(mood: Mood)
    suspend fun clearMood()
    val settings: Flow<AppSettings>
    suspend fun setReadAloud(enabled: Boolean)
    suspend fun setVoiceInput(enabled: Boolean)
    suspend fun setFollowMoodTheme(enabled: Boolean)
    suspend fun setOnboarded()
    val checkInStats: Flow<CheckInStats>
    suspend fun recordCheckIn(mood: Mood, feeling: Feeling)
    suspend fun clearCheckIns()
    val reminder: Flow<ReminderSettings>
    suspend fun setReminder(settings: ReminderSettings)
}
