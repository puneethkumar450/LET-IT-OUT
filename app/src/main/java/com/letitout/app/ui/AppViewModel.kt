package com.letitout.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.letitout.app.auth.AuthManager
import com.letitout.app.data.ChatMessage
import com.letitout.app.data.ChatRepository
import com.letitout.app.data.Mood
import com.letitout.app.data.MoodPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val preferences: MoodPreferences,
    val auth: AuthManager
) : ViewModel() {
    data class UiState(
        val mood: Mood = Mood.TALK,
        val messages: List<ChatMessage> = emptyList(),
        val loading: Boolean = false,
        val error: String? = null,
        val authConfigured: Boolean = false,
        val signedIn: Boolean = false
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()
    private var nextId = 1L

    init {
        refreshAuth()
        viewModelScope.launch {
            preferences.mood.collect { mood -> _state.update { it.copy(mood = mood) } }
        }
    }

    fun refreshAuth() = _state.update {
        it.copy(authConfigured = auth.isConfigured(), signedIn = auth.isSignedIn())
    }

    fun chooseMood(mood: Mood) {
        viewModelScope.launch { preferences.setMood(mood) }
        _state.update { it.copy(mood = mood, messages = emptyList(), error = null) }
    }

    fun clearConversation() = _state.update { it.copy(messages = emptyList(), error = null) }
    fun dismissError() = _state.update { it.copy(error = null) }

    fun send(text: String) {
        val clean = text.trim().take(2000)
        if (clean.isBlank() || _state.value.loading) return
        val withUser = _state.value.messages + ChatMessage(nextId++, clean, true)
        _state.update { it.copy(messages = withUser, loading = true, error = null) }
        viewModelScope.launch {
            try {
                val answer = repository.reply(_state.value.mood, withUser)
                _state.update { it.copy(messages = it.messages + ChatMessage(nextId++, answer, false), loading = false) }
            } catch (cancel: CancellationException) {
                _state.update { it.copy(loading = false) }
                throw cancel
            } catch (t: Exception) {
                _state.update { it.copy(loading = false, error = t.message ?: "Could not connect. Please try again.") }
            }
        }
    }

    fun retry() {
        val last = _state.value.messages.lastOrNull { it.fromUser } ?: return
        // Retry the request without creating a duplicate user message.
        if (_state.value.loading) return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val answer = repository.reply(_state.value.mood, _state.value.messages)
                _state.update { it.copy(messages = it.messages + ChatMessage(nextId++, answer, false), loading = false) }
            } catch (cancel: CancellationException) {
                _state.update { it.copy(loading = false) }
                throw cancel
            } catch (t: Exception) {
                _state.update { it.copy(loading = false, error = t.message ?: "Please try again.") }
            }
        }
    }
}
