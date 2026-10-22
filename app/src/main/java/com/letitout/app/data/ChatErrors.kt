package com.letitout.app.data

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.HttpException
import java.io.IOException
import java.io.InterruptedIOException

/** An error whose message is already written for the person using the app. */
class FriendlyException(message: String, cause: Throwable? = null) : Exception(message, cause)

private const val FALLBACK = "Something went wrong on my side. Please try again."
private const val OFFLINE = "Couldn't reach the server. Check your connection and try again."
private const val SLOW = "That took longer than it should have. Please try again."

private val errorAdapter: JsonAdapter<ErrorBody> by lazy {
    Moshi.Builder().add(KotlinJsonAdapterFactory()).build().adapter(ErrorBody::class.java)
}

/**
 * Turns any failure into a short, human sentence for the chat screen.
 *
 * Policy for the backend's `{"error": "..."}` text: it is shown only for 429 (slow down) and 503 (chat unavailable),
 * where the server knows best what is going on, and only when it is a short non-blank sentence. Every other status
 * uses the app's own copy so the advice always matches what the app can actually do.
 */
fun friendlyMessage(t: Throwable): String = when (t) {
    is FriendlyException -> t.message?.takeIf { it.isNotBlank() } ?: FALLBACK
    is HttpException -> httpMessage(t)
    is InterruptedIOException -> SLOW // SocketTimeoutException, and OkHttp's call timeout ("timeout")
    is IOException -> OFFLINE
    else -> FALLBACK
}

/**
 * For a failed reply report: the specific sentence when it tells the person something useful (signed out, sign-in needs a
 * refresh, or the hourly report limit), otherwise null so the caller can use its own "try again" note.
 */
fun reportFailureMessage(t: Throwable): String? = when {
    t is FriendlyException -> friendlyMessage(t)
    t is HttpException && t.code() in setOf(401, 403, 429) -> friendlyMessage(t)
    else -> null
}

private fun httpMessage(e: HttpException): String = when (val code = e.code()) {
    401, 403 -> "Your sign-in needs a refresh. Please sign out and sign in again, then we can keep talking."
    413 -> "That was a lot to send in one go. Could you try a shorter message?"
    400 -> "That message didn't go through. Mind trying again?"
    429 -> serverText(e) ?: "We've talked a lot this past hour. Take a short pause and come back in a few minutes."
    503 -> serverText(e) ?: "Chat is taking a short break right now. Please try again in a little while."
    else -> if (code >= 500) "I couldn't put a reply together just now. Mind trying again?" else FALLBACK
}

private fun serverText(e: HttpException): String? = runCatching {
    e.response()?.errorBody()?.string()?.let { errorAdapter.fromJson(it)?.error }
}.getOrNull()?.trim()?.takeIf { it.isNotEmpty() && it.length <= 240 }
