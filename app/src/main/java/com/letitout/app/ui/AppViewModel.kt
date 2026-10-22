package com.letitout.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.letitout.app.data.AuthGateway
import com.letitout.app.data.ChatGateway
import com.letitout.app.data.ChatMessage
import com.letitout.app.data.CheckInStats
import com.letitout.app.data.Feeling
import com.letitout.app.data.Mood
import com.letitout.app.data.ReminderSettings
import com.letitout.app.data.ReportReason
import com.letitout.app.data.UserPreferences
import com.letitout.app.data.friendlyMessage
import com.letitout.app.data.reportFailureMessage
import com.letitout.app.reminder.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val chat: ChatGateway,
    private val preferences: UserPreferences,
    val auth: AuthGateway,
    private val reminders: ReminderScheduler
) : ViewModel() {
    data class UiState(
        val mood: Mood = Mood.TALK,
        val messages: List<ChatMessage> = emptyList(),
        val loading: Boolean = false,
        val error: String? = null,
        val authConfigured: Boolean = false,
        val signedIn: Boolean = false,
        val userName: String? = null,
        val supportSuggested: Boolean = false,
        val supportOffered: Boolean = false,    // the support card was shown at some point in this conversation
        val feelingCheckDue: Boolean = false,
        val reportNotice: String? = null,       // "thanks" (or "couldn't send") after flagging a reply
        val checkInStats: CheckInStats = CheckInStats(),
        val reminder: ReminderSettings = ReminderSettings()
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()
    private var nextId = 1L

    // Each new conversation bumps the generation; replies from an older one are dropped.
    private var generation = 0
    private var replyJob: Job? = null
    // Companion-reply count when "Not now" was tapped; -1 = not dismissed in this conversation.
    private var dismissedAtReplyCount = -1
    private var feelingAnswered = false

    init {
        refreshAuth()
        viewModelScope.launch { preferences.mood.collect { mood -> _state.update { it.copy(mood = mood) } } }
        viewModelScope.launch { preferences.checkInStats.collect { stats -> _state.update { it.copy(checkInStats = stats) } } }
        viewModelScope.launch { preferences.reminder.collect { reminder -> _state.update { it.copy(reminder = reminder) } } }
    }

    fun refreshAuth() {
        _state.update { it.copy(authConfigured = auth.isConfigured(), signedIn = auth.isSignedIn(), userName = auth.userName()) }
    }

    fun chooseMood(mood: Mood) {
        viewModelScope.launch { preferences.setMood(mood) }
        startNewConversation(mood)
    }

    fun clearConversation() = startNewConversation(_state.value.mood)

    fun send(text: String) {
        val clean = text.trim().take(2000)
        if (clean.isBlank() || _state.value.loading) return
        val withUser = _state.value.messages + ChatMessage(nextId++, clean, true)
        _state.update { it.copy(messages = withUser) }
        requestReply(withUser)
    }

    /** Asks again for the last unanswered message without adding it a second time. */
    fun retry() {
        val current = _state.value
        if (current.loading || current.messages.lastOrNull()?.fromUser != true) return
        requestReply(current.messages)
    }

    fun dismissError() = _state.update { it.copy(error = null) }

    fun dismissSupport() {
        dismissedAtReplyCount = _state.value.messages.count { !it.fromUser }
        _state.update { it.copy(supportSuggested = false) }
    }

    /** Sends a reply the person flagged, then shows a short thank-you (or a gentle retry note). */
    fun reportReply(replyText: String, reason: ReportReason) {
        val requestGeneration = generation
        viewModelScope.launch {
            val notice = try {
                chat.report(reason, replyText)
                REPORT_THANKS
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (t: Exception) {
                reportFailureMessage(t) ?: REPORT_FAILED
            }
            if (requestGeneration == generation) _state.update { it.copy(reportNotice = notice) }
        }
    }

    fun dismissReportNotice() = _state.update { it.copy(reportNotice = null) }

    fun recordFeeling(feeling: Feeling) {
        if (feelingAnswered) return
        val mood = _state.value.mood
        finishFeelingCheck()
        viewModelScope.launch { preferences.recordCheckIn(mood, feeling) }
    }

    fun skipFeelingCheck() = finishFeelingCheck()

    fun setReminder(enabled: Boolean, hour: Int, minute: Int) {
        val settings = ReminderSettings(enabled, hour.coerceIn(0, 23), minute.coerceIn(0, 59))
        _state.update { it.copy(reminder = settings) }
        if (settings.enabled) reminders.schedule(settings.hour, settings.minute) else reminders.cancel()
        viewModelScope.launch { preferences.setReminder(settings) }
    }

    fun clearCheckIns() {
        _state.update { it.copy(checkInStats = CheckInStats()) }
        viewModelScope.launch { preferences.clearCheckIns() }
    }

    /** On sign-out or account deletion: turn off the daily check-in and clear check-ins kept on this phone. */
    fun clearLocalData() {
        val reminder = _state.value.reminder
        setReminder(false, reminder.hour, reminder.minute)
        clearCheckIns()
    }

    /**
     * Signs out and clears this phone's check-ins, reminder and conversation. Runs in the ViewModel, so leaving or
     * rotating Settings halfway can't skip the clean-up after Firebase has already signed out.
     */
    fun signOut(onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                auth.signOut()
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (ignored: Exception) {
                // Best effort; refreshAuth below shows whatever state sign-out actually reached.
            } finally {
                clearLocalData(); clearConversation(); refreshAuth()
            }
            onDone()
        }
    }

    /** Deletes the account; local data is cleared once nobody is signed in. [onError] gets a friendly sentence. */
    fun deleteAccount(onDone: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val error = try {
                auth.deleteAccount()
                null
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (t: Exception) {
                friendlyMessage(t)
            }
            if (!auth.isSignedIn()) { clearConversation(); clearLocalData() }
            refreshAuth()
            if (error == null) onDone() else onError(error)
        }
    }

    private fun startNewConversation(mood: Mood) {
        generation++
        replyJob?.cancel()
        replyJob = null
        dismissedAtReplyCount = -1
        feelingAnswered = false
        _state.update {
            it.copy(
                mood = mood, messages = emptyList(), loading = false, error = null,
                supportSuggested = false, supportOffered = false, feelingCheckDue = false, reportNotice = null
            )
        }
    }

    private fun finishFeelingCheck() {
        feelingAnswered = true
        _state.update { it.copy(feelingCheckDue = false) }
    }

    private fun requestReply(conversation: List<ChatMessage>) {
        val requestGeneration = generation
        val mood = _state.value.mood
        _state.update { it.copy(loading = true, error = null) }
        replyJob = viewModelScope.launch {
            try {
                val reply = chat.reply(mood, conversation)
                if (requestGeneration != generation) return@launch
                val answer = ChatMessage(nextId++, reply.text, false)
                _state.update {
                    val messages = it.messages + answer
                    val replies = messages.count { message -> !message.fromUser }
                    // After "Not now" the card stays away for a few replies, but comes back if things still sound heavy.
                    val showSupport = it.supportSuggested || (reply.supportSuggested &&
                        (dismissedAtReplyCount < 0 || replies - dismissedAtReplyCount >= SUPPORT_RESHOW_AFTER))
                    it.copy(
                        messages = messages,
                        loading = false,
                        supportSuggested = showSupport,
                        supportOffered = it.supportOffered || showSupport,
                        feelingCheckDue = !feelingAnswered && replies >= 2
                    )
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (t: Exception) {
                if (requestGeneration != generation) return@launch
                _state.update { it.copy(loading = false, error = friendlyMessage(t)) }
            }
        }
    }

    private companion object {
        const val SUPPORT_RESHOW_AFTER = 4
        const val REPORT_THANKS = "Thanks for telling us. It helps us make this space better."
        const val REPORT_FAILED = "Couldn't send that just now. Please try again in a moment."
    }
}
