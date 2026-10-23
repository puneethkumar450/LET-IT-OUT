package com.letitout.app.data

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ChatErrorsTest {
    private fun http(code: Int, body: String = "") =
        HttpException(Response.error<Any>(code, body.toResponseBody("application/json".toMediaType())))

    private val bannedWords = listOf(
        "crisis", "emergency", "suicid", "kill", "die", "death", "danger", "life-threatening", "are you safe", "http", "error"
    )

    private fun assertHuman(message: String) {
        assertTrue("message should not be blank", message.isNotBlank())
        bannedWords.forEach { assertFalse("\"$message\" contains \"$it\"", message.lowercase().contains(it)) }
    }

    @Test fun unauthorizedAsksToSignInAgain() {
        val message = friendlyMessage(http(401, """{"error":"Please sign in again."}"""))
        assertTrue(message.contains("sign in again"))
        assertHuman(message)
    }

    @Test fun tooLargeAsksForShorterMessage() {
        val message = friendlyMessage(http(413))
        assertTrue(message.contains("shorter"))
        assertHuman(message)
    }

    @Test fun rateLimitUsesServerSentenceWhenPresent() {
        val server = "You've sent quite a few messages. Let's pause for a bit and pick this up soon."
        assertEquals(server, friendlyMessage(http(429, """{"error":"$server"}""")))
    }

    @Test fun rateLimitFallsBackToAppCopy() {
        val message = friendlyMessage(http(429))
        assertTrue(message.contains("pause"))
        assertHuman(message)
        assertEquals(message, friendlyMessage(http(429, """{"error":"   "}""")))
        assertEquals(message, friendlyMessage(http(429, "<html>Too Many Requests</html>")))
        assertEquals(message, friendlyMessage(http(429, """{"error":"${"x".repeat(300)}"}""")))
    }

    @Test fun unavailableUsesServerSentenceWhenPresent() {
        val server = "Chat isn't available right now. Please try again a bit later."
        assertEquals(server, friendlyMessage(http(503, """{"error":"$server"}""")))
        val fallback = friendlyMessage(http(503))
        assertTrue(fallback.contains("short break"))
        assertHuman(fallback)
    }

    @Test fun serverFailuresUseAppCopyEvenWithBody() {
        val message = friendlyMessage(http(500, """{"error":"Internal details"}"""))
        assertEquals("I couldn't put a reply together just now. Mind trying again?", message)
        assertEquals(message, friendlyMessage(http(502, """{"error":"Upstream said no"}""")))
    }

    @Test fun timeoutsSayItTookTooLong() {
        val message = friendlyMessage(SocketTimeoutException("read timed out"))
        assertTrue(message.contains("longer than it should"))
        assertEquals(message, friendlyMessage(InterruptedIOException("timeout")))
        assertHuman(message)
    }

    @Test fun networkFailuresSuggestCheckingConnection() {
        val message = friendlyMessage(UnknownHostException("api.example.com"))
        assertEquals("Couldn't reach the server. Check your connection and try again.", message)
        assertEquals(message, friendlyMessage(IOException("reset")))
    }

    @Test fun friendlyExceptionPassesThrough() {
        assertEquals("Please sign in first.", friendlyMessage(FriendlyException("Please sign in first.")))
    }

    @Test fun reportFailuresKeepOnlyTheUsefulDetails() {
        val limit = "Thanks, we already have your recent reports. Please try again a little later."
        assertEquals(limit, reportFailureMessage(http(429, """{"error":"$limit"}""")))
        assertTrue(reportFailureMessage(http(401)).orEmpty().contains("sign in again"))
        assertTrue(reportFailureMessage(http(403)).orEmpty().contains("sign in again"))
        assertEquals("Please sign in first.", reportFailureMessage(FriendlyException("Please sign in first.")))
        assertNull(reportFailureMessage(http(503)))
        assertNull(reportFailureMessage(http(500)))
        assertNull(reportFailureMessage(IOException("offline")))
    }

    @Test fun unknownFailuresStayGentleAndHideDetails() {
        val message = friendlyMessage(IllegalStateException("NullPointer at line 42"))
        assertFalse(message.contains("42"))
        assertHuman(message)
    }
}
