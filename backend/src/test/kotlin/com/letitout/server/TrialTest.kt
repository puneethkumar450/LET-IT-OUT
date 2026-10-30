package com.letitout.server

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TrialTest {
    private val rules = TrialRules()
    private val policy = TrialPolicy(rules)
    /** 10:00 on 29 October 2026, India time. */
    private val morning = ZonedDateTime.of(2026, 10, 29, 10, 0, 0, 0, ZoneId.of("Asia/Kolkata")).toEpochSecond()

    private fun TrialState.chat(id: String, at: Long, closing: Boolean = false) = policy.chat(this, id, at, closing)

    @Test fun threeChatsADayThenTheDailyLimit() {
        var state = TrialState()
        listOf("chat-0001", "chat-0002", "chat-0003").forEach { id ->
            val (next, gate) = state.chat(id, morning)
            assertEquals(ChatGate.Open(rules.chatSeconds), gate)
            state = next
        }
        // Later messages in an open chat don't count again.
        assertEquals(ChatGate.Open(rules.chatSeconds - 60), state.chat("chat-0003", morning + 60).second)
        assertEquals(ChatGate.DailyLimit, state.chat("chat-0004", morning).second)
        assertEquals(0, policy.allowance(state, morning).chatsLeft)

        // Midnight India time resets the count.
        val tomorrow = ZonedDateTime.of(2026, 10, 30, 0, 1, 0, 0, ZoneId.of("Asia/Kolkata")).toEpochSecond()
        assertEquals(3, policy.allowance(state, tomorrow).chatsLeft)
        assertIs<ChatGate.Open>(state.chat("chat-0004", tomorrow).second)
    }

    @Test fun aChatEndsAfterFifteenMinutesWithOneGoodbye() {
        val (state, _) = TrialState().chat("chat-0001", morning)
        val end = morning + rules.chatSeconds
        assertEquals(ChatGate.SessionEnded, state.chat("chat-0001", end).second)
        assertEquals(ChatGate.SessionEnded, state.chat("chat-0001", end - 120, closing = true).second)
        val (closed, gate) = state.chat("chat-0001", end + 5, closing = true)
        assertEquals(ChatGate.Closing, gate)
        assertEquals(ChatGate.SessionEnded, closed.chat("chat-0001", end + 10, closing = true).second)
        assertEquals(ChatGate.SessionEnded, state.chat("chat-0001", end + rules.closingGraceSeconds + 1, closing = true).second)
    }

    @Test fun supportChatsHaveNoTimerAndDoNotCount() {
        var state = TrialState().chat("chat-0001", morning).first
        state = policy.support(state, "chat-0001", morning + 60)
        assertEquals(ChatGate.Open(null), state.chat("chat-0001", morning + 3 * 3600).second)
        assertEquals(3, policy.allowance(state, morning).chatsLeft)
        // Even with today's chats used, a support chat can always open.
        listOf("a-chat-01", "a-chat-02", "a-chat-03").forEach { state = state.chat(it, morning).first }
        state = policy.support(state, "chat-0009", morning)
        assertEquals(ChatGate.Open(null), state.chat("chat-0009", morning).second)
    }

    @Test fun oneLivePreviewEverySevenDays() {
        var state = TrialState()
        val (started, gate) = policy.live(state, "chat-0001", morning)
        assertEquals(LiveGate.Open(rules.liveSeconds), gate)
        // Opening live started the chat, so it counts and its timer runs.
        assertEquals(2, policy.allowance(started, morning).chatsLeft)
        state = policy.confirmLive(started, "chat-0001", morning + 5).first
        // Reconnecting continues the same preview, timed from when the video appeared.
        assertEquals(LiveGate.Open(rules.liveSeconds - 60), policy.live(state, "chat-0001", morning + 65).second)
        assertIs<LiveGate.Used>(policy.live(state, "chat-0001", morning + 5 + rules.liveSeconds).second)
        val other = state.chat("chat-0002", morning + 600).first
        assertEquals(LiveGate.Used(morning + 5 + rules.liveEverySeconds), policy.live(other, "chat-0002", morning + 600).second)
        assertFalse(policy.allowance(state, morning).liveAvailable)
        assertTrue(policy.allowance(state, morning + 5 + rules.liveEverySeconds).liveAvailable)
    }

    @Test fun aPreviewThatNeverShowsVideoIsGivenBack() {
        var state = TrialState().chat("chat-0001", morning).first
        state = policy.live(state, "chat-0001", morning).first
        // No confirmation: the week's preview is still available, in this chat or another.
        assertTrue(policy.allowance(state, morning + 30).liveAvailable)
        assertEquals(LiveGate.Open(rules.liveSeconds), policy.live(state, "chat-0001", morning + 30).second)
        // But unconfirmed attempts are capped, so the refund can't be used for unlimited video.
        state = policy.live(state, "chat-0001", morning + 30).first
        state = policy.live(state, "chat-0001", morning + 60).first
        assertEquals(LiveGate.Used(morning + rules.liveEverySeconds), policy.live(state, "chat-0001", morning + 90).second)
        assertFalse(policy.allowance(state, morning + 90).liveAvailable)
        // Confirming without a started preview does nothing.
        assertFalse(policy.confirmLive(TrialState(), "chat-0009", morning).second)
    }

    @Test fun liveRouteCountsOnlyAfterTheAppConfirmsVideo() = testApplication {
        val now = morning
        application {
            module(ServerConfig.fromEnvironment(emptyMap()), ChatDependencies(
                ready = { true },
                verifyUid = { _, _ -> "trial-user" },
                clock = { now }
            ), AvatarDependencies(
                ready = { true },
                verifyUid = { _, _ -> "trial-user" },
                createSession = { _, _, seconds -> assertEquals(180L, seconds); "token" },
                rateLimiter = { object : RateLimiter { override suspend fun allow(uid: String) = true } }
            ))
        }
        suspend fun allowance() = client.get("/v1/allowance") { header(HttpHeaders.Authorization, "Bearer valid") }.bodyAsText()
        val session = client.post("/v1/avatar/session?sessionId=chat-0001") { header(HttpHeaders.Authorization, "Bearer valid") }
        assertTrue(session.bodyAsText().contains("\"seconds\":180"))
        assertTrue(allowance().contains("\"liveAvailable\":true"))
        assertEquals(HttpStatusCode.NotFound, client.post("/v1/avatar/started?sessionId=chat-0002") { header(HttpHeaders.Authorization, "Bearer valid") }.status)
        assertEquals(HttpStatusCode.NoContent, client.post("/v1/avatar/started?sessionId=chat-0001") { header(HttpHeaders.Authorization, "Bearer valid") }.status)
        assertTrue(allowance().contains("\"liveAvailable\":false"))
    }

    @Test fun liveNeverOutlastsTheChat() {
        val state = TrialState().chat("chat-0001", morning).first
        assertEquals(LiveGate.Open(60), policy.live(state, "chat-0001", morning + rules.chatSeconds - 60).second)
        assertEquals(LiveGate.SessionEnded, policy.live(state, "chat-0001", morning + rules.chatSeconds).second)
    }

    @Test fun routeReportsDailyLimitSessionEndAndBusy() = testApplication {
        var now = morning
        var providerCalls = 0
        application {
            module(ServerConfig.fromEnvironment(emptyMap()), ChatDependencies(
                ready = { true },
                verifyUid = { _, _ -> "trial-user" },
                reply = { req, _ -> providerCalls++; if (req.closing) "Goodbye for now." else "I'm listening." },
                safetyCheck = { _, _ -> false },
                rateLimiter = { object : RateLimiter { override suspend fun allow(uid: String) = true } },
                clock = { now }
            ))
        }
        suspend fun send(session: String, closing: Boolean = false, text: String = "Hello") = client.post("/v1/chat") {
            header(HttpHeaders.Authorization, "Bearer valid")
            contentType(ContentType.Application.Json)
            val last = if (closing) """{"role":"assistant","content":"I'm listening."}""" else """{"role":"user","content":"$text"}"""
            setBody("""{"mood":"SAD","messages":[$last],"sessionId":"$session","closing":$closing}""")
        }

        val first = send("chat-0001")
        assertTrue(first.bodyAsText().contains("\"secondsLeft\":900"))
        send("chat-0002"); send("chat-0003")
        val limited = send("chat-0004")
        assertEquals(HttpStatusCode.Forbidden, limited.status)
        assertTrue(limited.bodyAsText().contains("daily_limit"))

        // Crisis support still answers when today's chats are used, without the provider.
        val calls = providerCalls
        val crisis = send("chat-0005", text = "I want to die")
        assertEquals(HttpStatusCode.OK, crisis.status)
        assertTrue(crisis.bodyAsText().contains("CRISIS_SUPPORT"))
        assertEquals(calls, providerCalls)

        now += 15 * 60
        val ended = send("chat-0001")
        assertEquals(HttpStatusCode.Forbidden, ended.status)
        assertTrue(ended.bodyAsText().contains("session_ended"))
        val goodbye = send("chat-0001", closing = true)
        assertEquals(HttpStatusCode.OK, goodbye.status)
        assertTrue(goodbye.bodyAsText().contains("\"ended\":true"))

        val allowance = client.get("/v1/allowance") { header(HttpHeaders.Authorization, "Bearer valid") }
        assertTrue(allowance.bodyAsText().contains("\"chatsLeft\":0"))
    }

    @Test fun globalLimitAndProviderRateLimitShowAsBusy() = testApplication {
        application {
            module(ServerConfig.fromEnvironment(emptyMap()), ChatDependencies(
                ready = { true },
                verifyUid = { _, _ -> "trial-user" },
                reply = { _, _ -> "unused" },
                safetyCheck = { _, _ -> false },
                rateLimiter = { object : RateLimiter { override suspend fun check(uid: String) = RateLimit.GLOBAL } }
            ))
        }
        val response = client.post("/v1/chat") {
            header(HttpHeaders.Authorization, "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody("""{"mood":"SAD","messages":[{"role":"user","content":"Hello"}],"sessionId":"chat-0001"}""")
        }
        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
        assertTrue(response.bodyAsText().contains("\"code\":\"busy\""))
    }
}
