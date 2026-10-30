package com.letitout.server

import com.google.cloud.Timestamp
import com.google.firebase.cloud.FirestoreClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.ZoneId

/**
 * Free trial limits. A chat counts once its first message arrives and stays open for [chatSeconds],
 * plus [closingGraceSeconds] for Ari's goodbye. A live preview takes up to [liveSeconds] of that
 * chat, once every [liveEverySeconds]. Chats that open crisis support never count and have no timer.
 *
 * A live preview only counts once the app confirms Ari's video appeared, so a connection that fails
 * doesn't use up the week's preview. Unconfirmed attempts are capped at [liveAttemptsPerWindow] per
 * window, because the confirmation comes from the app and can't be checked.
 */
internal data class TrialRules(
    val chatsPerDay: Int = 3,
    val chatSeconds: Long = 15 * 60,
    val closingGraceSeconds: Long = 2 * 60,
    val liveSeconds: Long = 3 * 60,
    val liveEverySeconds: Long = 7 * 24 * 3600,
    val liveAttemptsPerWindow: Int = 3,
    val zone: ZoneId = ZoneId.of("Asia/Kolkata")
) {
    /** Bounds the stored list; support chats don't count but still take a slot. */
    val maxSessionsPerDay: Int get() = chatsPerDay + 7
}

@Serializable internal data class TrialSession(
    val id: String,
    val startedAt: Long,
    val counts: Boolean = true,
    val support: Boolean = false,
    val closed: Boolean = false,
    /** When a live session token was last issued for this chat. */
    val liveStartedAt: Long? = null,
    /** When the app reported Ari's video appearing; the preview's time runs from here. */
    val liveConfirmedAt: Long? = null
)

@Serializable internal data class TrialState(
    val day: String = "",
    val sessions: List<TrialSession> = emptyList(),
    /** When the last confirmed live preview began. */
    val lastLiveAt: Long? = null,
    /** When live session tokens were issued, kept for one window, to cap unconfirmed attempts. */
    val liveAttempts: List<Long> = emptyList()
)

internal sealed interface ChatGate {
    /** [secondsLeft] is null when the chat has no time limit (crisis support). */
    data class Open(val secondsLeft: Long?) : ChatGate
    data object Closing : ChatGate
    data object DailyLimit : ChatGate
    data object SessionEnded : ChatGate
}

internal sealed interface LiveGate {
    data class Open(val seconds: Long) : LiveGate
    data class Used(val nextAt: Long) : LiveGate
    data object DailyLimit : LiveGate
    data object SessionEnded : LiveGate
}

@Serializable internal data class AllowanceResponse(
    val chatsPerDay: Int,
    val chatsLeft: Int,
    val chatMinutes: Int,
    /** Epoch milliseconds of the next daily reset. */
    val resetsAt: Long,
    val liveSeconds: Int,
    val liveAvailable: Boolean,
    /** Epoch milliseconds when the next live preview opens; null when one is available now. */
    val nextLiveAt: Long?,
    /** False when this server has no live avatar configured. */
    val liveEnabled: Boolean = false
)

/** Pure rules over one user's [TrialState]; times are epoch seconds. */
internal class TrialPolicy(private val rules: TrialRules) {
    private fun dayOf(now: Long) = Instant.ofEpochSecond(now).atZone(rules.zone).toLocalDate()

    private fun TrialSession.endsAt() = startedAt + rules.chatSeconds

    /** Starts a new day, carrying over chats still in progress without counting them again. */
    fun rolled(state: TrialState, now: Long): TrialState {
        val today = dayOf(now).toString()
        if (state.day == today) return state
        val carried = state.sessions.filter {
            if (it.support) now - it.startedAt < 6 * 3600 else !it.closed && now < it.endsAt() + rules.closingGraceSeconds
        }.map { it.copy(counts = false) }
        return state.copy(day = today, sessions = carried)
    }

    private fun TrialState.counted() = sessions.count { it.counts }

    private fun TrialState.canStart() = counted() < rules.chatsPerDay && sessions.size < rules.maxSessionsPerDay

    private fun TrialState.replace(session: TrialSession) =
        copy(sessions = sessions.map { if (it.id == session.id) session else it })

    fun chat(state: TrialState, id: String, now: Long, closing: Boolean): Pair<TrialState, ChatGate> {
        val current = rolled(state, now)
        val session = current.sessions.firstOrNull { it.id == id }
        if (session == null) {
            if (closing) return current to ChatGate.SessionEnded
            if (!current.canStart()) return current to ChatGate.DailyLimit
            return current.copy(sessions = current.sessions + TrialSession(id, now)) to ChatGate.Open(rules.chatSeconds)
        }
        if (session.support) return current to ChatGate.Open(null)
        val left = session.endsAt() - now
        // A little early is fine: the phone's countdown and the server clock can differ by a few seconds.
        if (closing && left <= 30 && !session.closed && -left <= rules.closingGraceSeconds) {
            return current.replace(session.copy(closed = true)) to ChatGate.Closing
        }
        if (closing || left <= 0 || session.closed) return current to ChatGate.SessionEnded
        return current to ChatGate.Open(left)
    }

    /** Marks a chat as crisis support: no timer, and it stops counting toward today's chats. */
    fun support(state: TrialState, id: String, now: Long): TrialState {
        val current = rolled(state, now)
        val session = current.sessions.firstOrNull { it.id == id }
        if (session != null) return current.replace(session.copy(support = true, counts = false, closed = false))
        val kept = if (current.sessions.size >= rules.maxSessionsPerDay) current.sessions.drop(1) else current.sessions
        return current.copy(sessions = kept + TrialSession(id, now, counts = false, support = true))
    }

    /** When the next live preview opens, or null when one can start now. */
    private fun TrialState.nextLiveAt(now: Long): Long? {
        lastLiveAt?.plus(rules.liveEverySeconds)?.takeIf { it > now }?.let { return it }
        val recent = liveAttempts.filter { now - it < rules.liveEverySeconds }
        return if (recent.size >= rules.liveAttemptsPerWindow) recent.first() + rules.liveEverySeconds else null
    }

    fun live(state: TrialState, id: String, now: Long): Pair<TrialState, LiveGate> {
        val current = rolled(state, now)
        val session = current.sessions.firstOrNull { it.id == id }
        // Reconnecting within a preview that already showed video continues it.
        session?.liveConfirmedAt?.let { shown ->
            val left = rules.liveSeconds - (now - shown)
            return current to if (left > 10) LiveGate.Open(left) else LiveGate.Used(current.nextLiveAt(now) ?: (shown + rules.liveEverySeconds))
        }
        current.nextLiveAt(now)?.let { return current to LiveGate.Used(it) }
        val started = when {
            session == null && !current.canStart() -> return current to LiveGate.DailyLimit
            session == null -> TrialSession(id, now)
            !session.support && (session.closed || now >= session.endsAt()) -> return current to LiveGate.SessionEnded
            else -> session
        }
        val chatLeft = if (started.support) rules.liveSeconds else started.endsAt() - now
        val withLive = started.copy(liveStartedAt = now)
        val sessions = if (session == null) current.sessions + withLive else current.replace(withLive).sessions
        val attempts = current.liveAttempts.filter { now - it < rules.liveEverySeconds } + now
        return current.copy(sessions = sessions, liveAttempts = attempts) to LiveGate.Open(minOf(rules.liveSeconds, chatLeft))
    }

    /** Counts the preview once Ari's video has appeared. Returns false when no preview was started. */
    fun confirmLive(state: TrialState, id: String, now: Long): Pair<TrialState, Boolean> {
        val current = rolled(state, now)
        val session = current.sessions.firstOrNull { it.id == id }
        if (session?.liveStartedAt == null) return current to false
        if (session.liveConfirmedAt != null) return current to true
        return current.replace(session.copy(liveConfirmedAt = now)).copy(lastLiveAt = now) to true
    }

    fun allowance(state: TrialState, now: Long): AllowanceResponse {
        val current = rolled(state, now)
        val nextAt = current.nextLiveAt(now)
        val reset = dayOf(now).plusDays(1).atStartOfDay(rules.zone).toEpochSecond()
        return AllowanceResponse(
            chatsPerDay = rules.chatsPerDay,
            chatsLeft = (rules.chatsPerDay - current.counted()).coerceAtLeast(0),
            chatMinutes = (rules.chatSeconds / 60).toInt(),
            resetsAt = reset * 1000,
            liveSeconds = rules.liveSeconds.toInt(),
            liveAvailable = nextAt == null,
            nextLiveAt = nextAt?.times(1000)
        )
    }
}

internal interface TrialStore {
    /** Atomically reads one user's state, applies [change] and saves the result. */
    suspend fun <T> update(uid: String, change: (TrialState) -> Pair<TrialState, T>): T
}

/** Local development only: a restart resets every allowance. */
internal class LocalTrialStore : TrialStore {
    private val mutex = Mutex()
    private val states = mutableMapOf<String, TrialState>()

    override suspend fun <T> update(uid: String, change: (TrialState) -> Pair<TrialState, T>): T = mutex.withLock {
        val (next, result) = change(states[uid] ?: TrialState())
        states[uid] = next
        result
    }
}

/** One document per hashed UID; it expires after the live-preview window so no stale state lingers. */
internal class FirestoreTrialStore(private val rules: TrialRules, collectionName: String = "trial_allowances") : TrialStore {
    private val firestore = FirestoreClient.getFirestore()
    private val collection = firestore.collection(collectionName)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

    override suspend fun <T> update(uid: String, change: (TrialState) -> Pair<TrialState, T>): T = withContext(Dispatchers.IO) {
        val doc = collection.document(hashedUid(uid))
        firestore.runTransaction { transaction ->
            val stored = transaction.get(doc).get().getString("state")
            val state = stored?.let { runCatching { json.decodeFromString<TrialState>(it) }.getOrNull() } ?: TrialState()
            val (next, result) = change(state)
            if (next != state) {
                val expires = Instant.now().epochSecond + rules.liveEverySeconds + 24 * 3600
                transaction.set(doc, mapOf("state" to json.encodeToString(next), "expiresAt" to Timestamp.ofTimeSecondsAndNanos(expires, 0)))
            }
            result
        }.get()
    }
}

internal fun validSessionId(id: String?): Boolean = id != null && id.length in 8..64 && id.all { it.isLetterOrDigit() || it == '-' }

internal fun nowSeconds(): Long = Instant.now().epochSecond
