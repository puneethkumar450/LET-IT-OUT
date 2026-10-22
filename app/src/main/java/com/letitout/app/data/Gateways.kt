package com.letitout.app.data

import android.app.Activity
import kotlinx.coroutines.flow.Flow

interface AuthGateway {
    fun isConfigured(): Boolean
    fun isSignedIn(): Boolean
    fun userName(): String?
    suspend fun signInGuest()
    /** Returns true when signed in, false when the person closed the account picker. Throws [FriendlyException]. */
    suspend fun signInGoogle(activity: Activity): Boolean
    suspend fun signOut()
    /** Throws [FriendlyException], for example when Firebase needs a recent sign-in. */
    suspend fun deleteAccount()
}

interface ChatGateway {
    suspend fun reply(mood: Mood, conversation: List<ChatMessage>): ChatReply
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
