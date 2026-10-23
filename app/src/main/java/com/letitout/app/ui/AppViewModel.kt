package com.letitout.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.letitout.app.data.AppSettings
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
        val intensity: Int? = null,
        val messages: List<ChatMessage> = emptyList(),
        val loading: Boolean = false,
        val error: String? = null,
        val authConfigured: Boolean = false,
        val signedIn: Boolean = false,
        val guest: Boolean = false,
        val userName: String? = null,
        val userEmail: String? = null,
        val settings: AppSettings = AppSettings(),
        val feelingCheckDue: Boolean = false,
        val reportNotice: String? = null,       // "thanks" (or "couldn't send") after flagging a reply
        val checkInStats: CheckInStats = CheckInStats(),
        val reminder: ReminderSettings = ReminderSettings()
    ) {
        val supportMode: Boolean get() = messages.any { it.supportResources }
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()
    private var nextId = 1L

    // Each new conversation bumps the generation; replies from an older one are dropped.
    private var generation = 0
    private var replyJob: Job? = null
    private var feelingAnswered = false

    init {
        refreshAuth()
        viewModelScope.launch { preferences.mood.collect { mood -> _state.update { it.copy(mood = mood) } } }
        viewModelScope.launch { preferences.settings.collect { settings -> _state.update { it.copy(settings = settings) } } }
        viewModelScope.launch { preferences.checkInStats.collect { stats -> _state.update { it.copy(checkInStats = stats) } } }
        viewModelScope.launch { preferences.reminder.collect { reminder -> _state.update { it.copy(reminder = reminder) } } }
    }

    fun refreshAuth() = _state.update {
        it.copy(
            authConfigured = auth.isConfigured(), signedIn = auth.isSignedIn(), guest = auth.isGuest(),
            userName = auth.userName(), userEmail = auth.userEmail()
        )
    }

    fun chooseMood(mood: Mood, intensity: Int? = null) {
        viewModelScope.launch { preferences.setMood(mood) }
        startNewConversation(mood, intensity)
    }

    /** Starts over with the same mood and intensity. */
    fun newConversation() {
        val current = _state.value
        startNewConversation(current.mood, current.intensity)
    }

    /** Clears the conversation, the remembered mood and check-in history on this phone; settings and the reminder are kept. */
    fun clearDeviceData() {
        forgetConversationAndMood()
        clearCheckIns()
    }

    fun setReadAloud(enabled: Boolean) { viewModelScope.launch { preferences.setReadAloud(enabled) } }
    fun setVoiceInput(enabled: Boolean) { viewModelScope.launch { preferences.setVoiceInput(enabled) } }
    fun setFollowMoodTheme(enabled: Boolean) { viewModelScope.launch { preferences.setFollowMoodTheme(enabled) } }
    fun finishOnboarding() { viewModelScope.launch { preferences.setOnboarded() } }

    fun send(text: String) {
        val clean = text.trim().take(2000)
        if (clean.isBlank() || _state.value.loading) return
        val withUser = _state.value.messages + ChatMessage(nextId++, clean, true)
        _state.update { it.copy(messages = withUser, error = null) }
        requestReply(withUser)
    }

    /** Asks again for the last unanswered message without adding it a second time. */
    fun retry() {
        val current = _state.value
        if (current.loading || current.messages.lastOrNull()?.fromUser != true) return
        requestReply(current.messages)
    }

    /** Takes the unsent last message back out of the conversation so it can be edited. */
    fun editLastMessage(): String? {
        val current = _state.value
        if (current.loading) return null
        val last = current.messages.lastOrNull()?.takeIf { it.fromUser } ?: return null
        _state.update { it.copy(messages = it.messages.dropLast(1), error = null) }
        return last.text
    }

    fun dismissError() = _state.update { it.copy(error = null) }

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
                clearLocalData(); newConversation(); refreshAuth()
            }
            onDone()
        }
    }

    /**
     * Deletes the account; once nobody is signed in, the conversation, saved mood, check-ins and reminder on this phone
     * are cleared too. [onError] gets a friendly sentence.
     */
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
            if (!auth.isSignedIn()) { forgetConversationAndMood(); clearLocalData() }
            refreshAuth()
            if (error == null) onDone() else onError(error)
        }
    }

    private fun forgetConversationAndMood() {
        startNewConversation(Mood.TALK, null)
        viewModelScope.launch { preferences.clearMood() }
    }

    private fun startNewConversation(mood: Mood, intensity: Int?) {
        generation++
        replyJob?.cancel()
        replyJob = null
        feelingAnswered = false
        _state.update {
            it.copy(
                mood = mood, intensity = intensity, messages = emptyList(), loading = false, error = null,
                feelingCheckDue = false, reportNotice = null
            )
        }
    }

    private fun finishFeelingCheck() {
        feelingAnswered = true
        _state.update { it.copy(feelingCheckDue = false) }
    }

    // Due after the second reply, or while it is on its way (leaving then would otherwise skip the question).
    private fun feelingCheckDue(messages: List<ChatMessage>, waiting: Boolean) =
        !feelingAnswered && messages.count { !it.fromUser } >= (if (waiting) 1 else 2)

    private fun requestReply(conversation: List<ChatMessage>) {
        val requestGeneration = generation
        val current = _state.value
        _state.update { it.copy(loading = true, error = null, feelingCheckDue = feelingCheckDue(it.messages, waiting = true)) }
        replyJob = viewModelScope.launch {
            try {
                val reply = chat.reply(current.mood, conversation, current.intensity)
                if (requestGeneration != generation) return@launch
                val id = nextId++
                _state.update {
                    // The helpline note shows at most once every few replies, so a heavy stretch doesn't repeat it each time.
                    val recent = it.messages.filter { message -> !message.fromUser }.takeLast(SUPPORT_SPACING - 1)
                    val support = reply.supportSuggested && recent.none { message -> message.supportResources }
                    val messages = it.messages + ChatMessage(id, reply.text, false, supportResources = support)
                    it.copy(messages = messages, loading = false, feelingCheckDue = feelingCheckDue(messages, waiting = false))
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (t: Exception) {
                if (requestGeneration != generation) return@launch
                _state.update {
                    it.copy(loading = false, error = friendlyMessage(t), feelingCheckDue = feelingCheckDue(it.messages, waiting = false))
                }
            }
        }
    }

    private companion object {
        const val SUPPORT_SPACING = 4
        const val REPORT_THANKS = "Thanks for telling us. It helps us make this space better."
        const val REPORT_FAILED = "Couldn't send that just now. Please try again in a moment."
    }
}
