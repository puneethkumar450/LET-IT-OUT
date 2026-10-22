package com.letitout.app.data

import android.app.Activity
import kotlinx.coroutines.flow.Flow

interface AuthGateway {
    fun isConfigured(): Boolean
    fun isSignedIn(): Boolean
    fun userName(): String?
    /** True for an anonymous guest, who can upgrade to Google without losing anything on this phone. */
    fun isGuest(): Boolean = false
    suspend fun signInGuest()
    /** Returns true when signed in, false when the person closed the account picker. Throws [FriendlyException]. */
    suspend fun signInGoogle(activity: Activity): Boolean
    suspend fun signOut()
    /** Throws [FriendlyException], for example when Firebase needs a recent sign-in. */
    suspend fun deleteAccount()
}

interface ChatGateway {
    suspend fun reply(mood: Mood, conversation: List<ChatMessage>): ChatReply
    /** Sends one reply the person chose to flag, and why. Throws on failure. */
    suspend fun report(reason: ReportReason, replyText: String)
}

interface UserPreferences {
    val mood: Flow<Mood>
    suspend fun setMood(mood: Mood)
    val checkInStats: Flow<CheckInStats>
    suspend fun recordCheckIn(mood: Mood, feeling: Feeling)
    suspend fun clearCheckIns()
    val reminder: Flow<ReminderSettings>
    suspend fun setReminder(settings: ReminderSettings)
}
