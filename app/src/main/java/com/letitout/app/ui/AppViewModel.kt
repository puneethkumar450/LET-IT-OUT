package com.letitout.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.letitout.app.auth.AuthManager
import com.letitout.app.data.Allowance
import com.letitout.app.data.AppSettings
import com.letitout.app.data.AvatarSessionResponse
import com.letitout.app.data.ChatMessage
import com.letitout.app.data.ChatRepository
import com.letitout.app.data.Mood
import com.letitout.app.data.MoodPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import android.os.SystemClock
import javax.inject.Inject
import org.json.JSONObject

/** Why chat can't go on right now: today's free chats are used, or the shared free quota is. */
enum class TrialBlock { DAILY_LIMIT, BUSY }

/** A free-trial refusal from the server: daily_limit, session_ended, live_used or busy. */
data class TrialError(val code: String, val nextLiveAt: Long? = null)

/** Ari's goodbye when the server can't be reached for one. */
internal const val FALLBACK_GOODBYE =
    "We're at the end of our time for this chat. Thank you for sharing this with me. I'm here again whenever you need."

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
        val settings: AppSettings = AppSettings(),
        val allowance: Allowance? = null,
        /** [SystemClock.elapsedRealtime] when this chat's free time ends; null before it starts or with no limit. */
        val chatEndsAt: Long? = null,
        /** The chat's time is up and Ari has said goodbye; only Let it go or a new chat remain. */
        val timeUp: Boolean = false,
        val block: TrialBlock? = null
    ) {
        val supportMode: Boolean get() = messages.any { it.supportResources }
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()
    private var nextId = 1L
    private var replyJob: Job? = null
    private var warmupJob: Job? = null
    private var lastWarmupAt = 0L
    /** Invalidates callbacks from a request after a conversation reset or a newer request. */
    private val replyGeneration = AtomicLong(0L)
    /** Names the current chat for the server's free-trial limits; a new one starts with each conversation. */
    private var sessionId = newSessionId()
    private var timerJob: Job? = null

    init {
        refreshAuth()
        viewModelScope.launch {
            preferences.mood.collect { mood -> _state.update { it.copy(mood = mood) } }
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
        newConversation()
        _state.update { it.copy(mood = mood, intensity = intensity) }
    }

    fun newConversation() {
        cancelReply()
        timerJob?.cancel()
        sessionId = newSessionId()
        _state.update { it.copy(messages = emptyList(), loading = false, error = null, chatEndsAt = null, timeUp = false, block = null) }
        refreshAllowance()
    }

    /**
     * The release ritual: clears the conversation like [newConversation] and reports whether it
     * contained a crisis-support reply, so the end screen can lead with helplines.
     */
    fun releaseConversation(): Boolean {
        val hadSupport = _state.value.supportMode
        newConversation()
        return hadSupport
    }

    /** Clears the in-memory conversation and the remembered mood; settings are kept. */
    fun clearDeviceData() {
        newConversation()
        _state.update { it.copy(mood = Mood.TALK, intensity = null) }
        viewModelScope.launch { preferences.clearMood() }
    }

    /** Called only after Firebase confirms deletion; waits for local data to be removed. */
    suspend fun clearAllLocalData() = withContext(NonCancellable) {
        replyGeneration.incrementAndGet()
        replyJob?.cancelAndJoin()
        replyJob = null
        preferences.clearAll()
        _state.update {
            it.copy(
                mood = Mood.TALK, intensity = null, messages = emptyList(), loading = false,
                error = null, settings = AppSettings()
            )
        }
    }

    fun setReadAloud(enabled: Boolean) { viewModelScope.launch { preferences.setReadAloud(enabled) } }
    fun prewarmAri() {
        if (!_state.value.signedIn || warmupJob?.isActive == true) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastWarmupAt < 120_000L) return
        lastWarmupAt = now
        warmupJob = viewModelScope.launch {
            try { repository.prewarm() }
            catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) { /* Start talking still performs the normal connection and error flow. */ }
        }
    }
    /** Fetches today's allowance; a failure keeps the last known one. */
    fun refreshAllowance() {
        if (!_state.value.signedIn) return
        viewModelScope.launch {
            try {
                val allowance = repository.allowance()
                _state.update { it.copy(allowance = allowance) }
            } catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) { /* Home simply shows no allowance line. */ }
        }
    }

    /** Starts a live preview in this chat. Opening it starts the chat's timer if no message has. */
    suspend fun startLivePreview(mood: Mood): AvatarSessionResponse {
        val response = repository.avatarSession(mood, sessionId)
        val current = _state.value
        if (current.chatEndsAt == null && !current.supportMode && !current.timeUp) {
            val endsAt = SystemClock.elapsedRealtime() + (current.allowance?.chatMinutes ?: 15) * 60_000L
            _state.update { it.copy(chatEndsAt = endsAt) }
            scheduleTimeUp(endsAt)
        }
        refreshAllowance()
        return response
    }

    /**
     * Called when Ari's video first appears. Only then does the week's preview count, so a failed
     * connection doesn't use it up. If this call fails the preview simply isn't counted.
     */
    fun confirmLivePreview() {
        val id = sessionId
        viewModelScope.launch {
            try { repository.avatarStarted(id) }
            catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) { return@launch }
            refreshAllowance()
        }
    }

    /** Applies a live-preview refusal to the chat and returns the server's reason, if it gave one. */
    fun handleLiveFailure(t: Exception): TrialError? {
        val error = trialError(t)
        when (error?.code) {
            "daily_limit" -> _state.update { it.copy(block = TrialBlock.DAILY_LIMIT) }
            "busy" -> _state.update { it.copy(block = TrialBlock.BUSY) }
            "session_ended" -> onTimeUp()
        }
        return error
    }

    fun clearBlock() = _state.update { it.copy(block = null) }
    fun setVoiceInput(enabled: Boolean) { viewModelScope.launch { preferences.setVoiceInput(enabled) } }
    fun setFollowMoodTheme(enabled: Boolean) { viewModelScope.launch { preferences.setFollowMoodTheme(enabled) } }
    fun setReduceMotion(enabled: Boolean) { viewModelScope.launch { preferences.setReduceMotion(enabled) } }
    fun setAriVoice(name: String) { viewModelScope.launch { preferences.setAriVoice(name) } }
    fun finishOnboarding() { viewModelScope.launch { preferences.setOnboarded() } }

    fun send(text: String) {
        val clean = text.trim().take(2000)
        if (clean.isBlank() || _state.value.loading) return
        _state.update { it.copy(messages = it.messages + ChatMessage(nextId++, clean, true), error = null) }
        requestReply()
    }

    fun retry() {
        clearBlock()
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

    private fun requestReply(closing: Boolean = false) {
        val generation = replyGeneration.incrementAndGet()
        _state.update { it.copy(loading = true, error = null) }
        replyJob = viewModelScope.launch {
            try {
                val current = _state.value
                val response = repository.reply(current.mood, current.messages, current.intensity, sessionId, closing)
                if (generation == replyGeneration.get()) {
                    val support = response.safetyState != "NORMAL"
                    val reply = ChatMessage(nextId++, response.reply, false, supportResources = support)
                    val endsAt = when {
                        support || response.ended || closing -> null
                        response.secondsLeft != null -> SystemClock.elapsedRealtime() + response.secondsLeft * 1000
                        else -> current.chatEndsAt
                    }
                    val started = current.chatEndsAt == null && endsAt != null
                    _state.update {
                        if (generation == replyGeneration.get()) {
                            it.copy(messages = it.messages + reply, loading = false, chatEndsAt = endsAt, timeUp = it.timeUp || response.ended || closing)
                        } else it
                    }
                    if (started || support) refreshAllowance()
                    scheduleTimeUp(endsAt)
                }
            } catch (cancel: CancellationException) {
                _state.update { if (generation == replyGeneration.get()) it.copy(loading = false) else it }
                throw cancel
            } catch (t: Exception) {
                if (generation != replyGeneration.get()) return@launch
                val code = trialError(t)?.code
                when {
                    // Ari still says goodbye when the server can't; the chat ends either way.
                    closing -> _state.update {
                        it.copy(messages = it.messages + ChatMessage(nextId++, FALLBACK_GOODBYE, false), loading = false, chatEndsAt = null, timeUp = true)
                    }
                    code == "session_ended" -> {
                        _state.update { it.copy(loading = false) }
                        requestReply(closing = true)
                    }
                    code == "daily_limit" -> _state.update { it.copy(loading = false, block = TrialBlock.DAILY_LIMIT) }
                    code == "busy" -> _state.update { it.copy(loading = false, block = TrialBlock.BUSY) }
                    else -> {
                        _state.update { it.copy(loading = false, error = friendlyError(t)) }
                        scheduleTimeUp(_state.value.chatEndsAt)
                    }
                }
            }
        }
    }

    /** Waits for the chat's free time to end, then asks Ari for a goodbye. */
    private fun scheduleTimeUp(endsAt: Long?) {
        timerJob?.cancel()
        if (endsAt == null) return
        timerJob = viewModelScope.launch {
            delay((endsAt - SystemClock.elapsedRealtime()).coerceAtLeast(0L))
            onTimeUp()
        }
    }

    /** A reply still on its way finishes first; its result reschedules this check. */
    private fun onTimeUp() {
        val current = _state.value
        if (current.timeUp || current.supportMode || current.loading) return
        if (current.messages.isEmpty()) {
            _state.update { it.copy(timeUp = true, chatEndsAt = null) }
            return
        }
        requestReply(closing = true)
    }

    private fun cancelReply() {
        replyGeneration.incrementAndGet()
        replyJob?.cancel()
        replyJob = null
    }

    private fun newSessionId(): String = UUID.randomUUID().toString()

    private fun trialError(t: Exception): TrialError? {
        val body = (t as? HttpException)?.response()?.errorBody()?.string() ?: return null
        return try {
            val json = JSONObject(body)
            json.optString("code").takeIf { it.isNotEmpty() }?.let {
                TrialError(it, if (json.has("nextLiveAt") && !json.isNull("nextLiveAt")) json.getLong("nextLiveAt") else null)
            }
        } catch (_: Exception) { null }
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
