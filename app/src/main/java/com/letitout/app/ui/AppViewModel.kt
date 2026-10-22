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
import com.letitout.app.data.UserPreferences
import com.letitout.app.data.friendlyMessage
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
        val feelingCheckDue: Boolean = false,
        val checkInStats: CheckInStats = CheckInStats(),
        val reminder: ReminderSettings = ReminderSettings()
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()
    private var nextId = 1L

    // Each new conversation bumps the generation; replies from an older one are dropped.
    private var generation = 0
    private var replyJob: Job? = null
    private var supportDismissed = false
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
        supportDismissed = true
        _state.update { it.copy(supportSuggested = false) }
    }

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

    private fun startNewConversation(mood: Mood) {
        generation++
        replyJob?.cancel()
        replyJob = null
        supportDismissed = false
        feelingAnswered = false
        _state.update {
            it.copy(mood = mood, messages = emptyList(), loading = false, error = null, supportSuggested = false, feelingCheckDue = false)
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
                    it.copy(
                        messages = messages,
                        loading = false,
                        supportSuggested = it.supportSuggested || (reply.supportSuggested && !supportDismissed),
                        feelingCheckDue = !feelingAnswered && messages.count { message -> !message.fromUser } >= 2
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
}
