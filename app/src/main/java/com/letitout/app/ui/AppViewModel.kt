package com.letitout.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.letitout.app.auth.AuthManager
import com.letitout.app.data.AppSettings
import com.letitout.app.data.ChatMessage
import com.letitout.app.data.ChatRepository
import com.letitout.app.data.CompanionChoice
import com.letitout.app.data.Mood
import com.letitout.app.data.MoodPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val preferences: MoodPreferences,
    val auth: AuthManager
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
        val companion: CompanionChoice = CompanionChoice(),
        val settings: AppSettings = AppSettings()
    ) {
        val supportMode: Boolean get() = messages.any { it.supportResources }
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()
    private var nextId = 1L
    private var replyJob: Job? = null

    init {
        refreshAuth()
        viewModelScope.launch {
            preferences.mood.collect { mood -> _state.update { it.copy(mood = mood) } }
        }
        viewModelScope.launch {
            preferences.companion.collect { companion -> _state.update { it.copy(companion = companion) } }
        }
        viewModelScope.launch {
            preferences.settings.collect { settings -> _state.update { it.copy(settings = settings) } }
        }
    }

    fun refreshAuth() = _state.update {
        it.copy(
            authConfigured = auth.isConfigured(), signedIn = auth.isSignedIn(), guest = auth.isGuest(),
            userName = auth.userName(), userEmail = auth.userEmail()
        )
    }

    fun chooseMood(mood: Mood, intensity: Int? = null) {
        viewModelScope.launch { preferences.setMood(mood) }
        replyJob?.cancel()
        _state.update { it.copy(mood = mood, intensity = intensity, messages = emptyList(), loading = false, error = null) }
    }

    fun chooseCompanion(gender: String, age: String) {
        if (gender !in CompanionChoice.genders || age !in CompanionChoice.ages) return
        _state.update { it.copy(companion = CompanionChoice(gender, age)) }
        viewModelScope.launch { preferences.setCompanion(gender, age) }
    }

    fun newConversation() {
        replyJob?.cancel()
        _state.update { it.copy(messages = emptyList(), loading = false, error = null) }
    }

    /** Clears the in-memory conversation and the remembered mood; settings are kept. */
    fun clearDeviceData() {
        newConversation()
        _state.update { it.copy(mood = Mood.TALK, intensity = null) }
        viewModelScope.launch { preferences.clearMood() }
    }

    /** Called only after Firebase confirms deletion; waits for local data to be removed. */
    suspend fun clearAllLocalData() = withContext(NonCancellable) {
        replyJob?.cancelAndJoin()
        preferences.clearAll()
        _state.update {
            it.copy(
                mood = Mood.TALK, intensity = null, messages = emptyList(), loading = false,
                error = null, companion = CompanionChoice(), settings = AppSettings()
            )
        }
    }

    fun setReadAloud(enabled: Boolean) { viewModelScope.launch { preferences.setReadAloud(enabled) } }
    fun setVoiceInput(enabled: Boolean) { viewModelScope.launch { preferences.setVoiceInput(enabled) } }
    fun setFollowMoodTheme(enabled: Boolean) { viewModelScope.launch { preferences.setFollowMoodTheme(enabled) } }
    fun finishOnboarding() { viewModelScope.launch { preferences.setOnboarded() } }

    fun send(text: String) {
        val clean = text.trim().take(2000)
        if (clean.isBlank() || _state.value.loading) return
        _state.update { it.copy(messages = it.messages + ChatMessage(nextId++, clean, true), error = null) }
        requestReply()
    }

    fun retry() {
        // Retry the request without creating a duplicate user message.
        if (_state.value.loading || _state.value.messages.lastOrNull()?.fromUser != true) return
        requestReply()
    }

    /** Takes the unsent last message back out of the conversation so it can be edited. */
    fun editLastMessage(): String? {
        val last = _state.value.messages.lastOrNull()?.takeIf { it.fromUser } ?: return null
        _state.update { it.copy(messages = it.messages.dropLast(1), error = null) }
        return last.text
    }

    private fun requestReply() {
        _state.update { it.copy(loading = true, error = null) }
        replyJob = viewModelScope.launch {
            try {
                val current = _state.value
                val response = repository.reply(current.mood, current.messages, current.intensity)
                val reply = ChatMessage(nextId++, response.reply, false, supportResources = response.safetyState != "NORMAL")
                _state.update { it.copy(messages = it.messages + reply, loading = false) }
            } catch (cancel: CancellationException) {
                _state.update { it.copy(loading = false) }
                throw cancel
            } catch (t: Exception) {
                _state.update { it.copy(loading = false, error = friendlyError(t)) }
            }
        }
    }

    private fun friendlyError(t: Exception): String = when {
        t is IOException -> "I couldn't reach the server just now. Your message is still here — nothing was lost."
        t is HttpException && t.code() == 401 -> "Your sign-in has expired. Please sign in again, then try once more."
        t is HttpException && t.code() == 429 -> "Lots of messages right now. Please wait a moment and try again."
        t is HttpException && t.code() == 503 -> "The chat service isn't ready yet. Your message is still here — please try again later."
        t is HttpException -> "Something went wrong on our side. Your message is still here — nothing was lost."
        else -> t.message ?: "Something went wrong. Your message is still here — nothing was lost."
    }
}
