package com.letitout.server

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChatRouteTest {
    private val goodToken = "good-token"
    private val hour = 60 * 60 * 1000L
    private val verifier: suspend (String) -> String? = { if (it == goodToken) "uid-1" else null }
    private val heavy = "I don't want to be here anymore"

    private fun deps(
        provider: ReplyProvider? = FakeProvider(),
        verifyToken: (suspend (String) -> String?)? = verifier,
        limiter: RateLimiter = RateLimiter(20, hour),
        careLimiter: RateLimiter = RateLimiter(30, hour)
    ) = ChatDeps(verifyToken, provider, limiter, careLimiter)

    private fun chatJson(mood: String, messages: List<Message>) =
        Json.encodeToString(ChatRequest.serializer(), ChatRequest(mood, messages))

    private fun says(text: String, mood: String = "SAD") = chatJson(mood, listOf(Message("user", text)))

    private suspend fun ApplicationTestBuilder.chat(body: String, token: String? = goodToken): HttpResponse =
        client.post("/v1/chat") {
            if (token != null) header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(body)
        }

    private suspend fun HttpResponse.json(): JsonObject = Json.parseToJsonElement(bodyAsText()).jsonObject

    private suspend fun HttpResponse.assertFriendlyError(status: HttpStatusCode): String {
        assertEquals(status, this.status)
        assertTrue(contentType()?.match(ContentType.Application.Json) == true)
        val error = json()["error"]!!.jsonPrimitive.content
        assertTrue(error.isNotBlank() && error.endsWith("."), error)
        assertFalse("HTTP" in error || "error" in error.lowercase(), error)
        assertNoBannedWords(error)
        return error
    }

    private suspend fun HttpResponse.assertReply(safetyState: String): String {
        assertEquals(HttpStatusCode.OK, status)
        val body = json()
        assertEquals(safetyState, body["safetyState"]!!.jsonPrimitive.content)
        return body["reply"]!!.jsonPrimitive.content
    }

    @Test fun healthCheck() = testApplication {
        application { module(deps()) }
        assertEquals("ok", client.get("/health").bodyAsText())
    }

    @Test fun unconfiguredBackendReturns503BeforeAuth() = testApplication {
        application { module(deps(verifyToken = null)) }
        chat(says("hi"), token = null).assertFriendlyError(HttpStatusCode.ServiceUnavailable)
    }

    @Test fun missingProviderReturns503() = testApplication {
        application { module(deps(provider = null)) }
        chat(says("hi")).assertFriendlyError(HttpStatusCode.ServiceUnavailable)
    }

    @Test fun badOrMissingTokenReturns401() = testApplication {
        val provider = FakeProvider()
        application { module(deps(provider)) }
        chat(says("hi"), token = null).assertFriendlyError(HttpStatusCode.Unauthorized)
        chat(says("hi"), token = "nope").assertFriendlyError(HttpStatusCode.Unauthorized)
        assertTrue(provider.calls.isEmpty())
    }

    @Test fun throwingVerifierReturns401() = testApplication {
        application { module(deps(verifyToken = { throw IllegalStateException("expired") })) }
        chat(says("hi")).assertFriendlyError(HttpStatusCode.Unauthorized)
    }

    @Test fun bodyJustOverLimitReturns413() = testApplication {
        val provider = FakeProvider()
        application { module(deps(provider)) }
        val valid = says("hi")
        val oversized = valid.dropLast(1) + " ".repeat(MAX_BODY_CHARS + 1 - valid.length) + "}"
        assertEquals(MAX_BODY_CHARS + 1, oversized.length)
        chat(oversized).assertFriendlyError(HttpStatusCode.PayloadTooLarge)
        assertTrue(provider.calls.isEmpty())
    }

    @Test fun fullSizeConversationIsAccepted() = testApplication {
        val provider = FakeProvider()
        application { module(deps(provider)) }
        val english = (0 until 12).map { Message(if (it % 2 == 1) "user" else "assistant", "a".repeat(2000)) }
        chat(chatJson("STRESSED", english)).assertReply(NORMAL)
        assertEquals(12, provider.calls.single().second.size)
        // Telugu is three bytes per character, so the cap must be about characters, not bytes.
        val telugu = (0 until 12).map { Message(if (it % 2 == 1) "user" else "assistant", "అ".repeat(2000)) }
        chat(chatJson("TALK", telugu)).assertReply(NORMAL)
    }

    @Test fun invalidRequestsReturn400() = testApplication {
        val provider = FakeProvider()
        application { module(deps(provider)) }
        val bad = listOf(
            "not json",
            """{"mood":"SAD"}""",
            says("hi", mood = "BORED"),
            chatJson("SAD", emptyList()),
            chatJson("SAD", List(13) { Message(if (it % 2 == 0) "user" else "assistant", "hi") }),
            chatJson("SAD", listOf(Message("system", "hi"))),
            chatJson("SAD", listOf(Message("user", ""))),
            chatJson("SAD", listOf(Message("user", "a".repeat(2001)))),
            chatJson("SAD", listOf(Message("user", "hi"), Message("assistant", "hello")))
        )
        bad.forEach { chat(it).assertFriendlyError(HttpStatusCode.BadRequest) }
        assertTrue(provider.calls.isEmpty())
    }

    @Test fun normalReplyIsPolishedAndNormal() = testApplication {
        val provider = FakeProvider(reply = "\"Ugh, **again**? That sounds exhausting.\"")
        application { module(deps(provider)) }
        assertEquals("Ugh, again? That sounds exhausting.", chat(says("My boss yelled again", "ANGRY")).assertReply(NORMAL))
        val (prompt, messages) = provider.calls.single()
        assertEquals(Persona.systemPrompt("ANGRY", extraCare = false), prompt)
        assertEquals(listOf(Message("user", "My boss yelled again")), messages)
    }

    @Test fun twentyFirstNormalRequestGets429() = testApplication {
        val provider = FakeProvider()
        application { module(deps(provider)) }
        repeat(20) { chat(says("work was long, message $it")).assertReply(NORMAL) }
        val error = chat(says("one more")).assertFriendlyError(HttpStatusCode.TooManyRequests)
        assertEquals(FriendlyErrors.TOO_MANY, error)
        assertEquals(20, provider.calls.size)
    }

    @Test fun heavyMessageStillAnsweredAfterNormalQuotaIsUsed() = testApplication {
        val provider = FakeProvider()
        application { module(deps(provider, limiter = RateLimiter(1, hour))) }
        chat(says("hi")).assertReply(NORMAL)
        chat(says("hi again")).assertFriendlyError(HttpStatusCode.TooManyRequests)
        chat(says(heavy)).assertReply(SUPPORT_SUGGESTED)
        assertEquals(2, provider.calls.size)
        assertEquals(Persona.systemPrompt("SAD", extraCare = true), provider.calls.last().first)
    }

    @Test fun heavyMessagesDoNotUseNormalQuota() = testApplication {
        val provider = FakeProvider()
        application { module(deps(provider, limiter = RateLimiter(1, hour))) }
        chat(says(heavy)).assertReply(SUPPORT_SUGGESTED)
        chat(says(heavy)).assertReply(SUPPORT_SUGGESTED)
        chat(says("thanks")).assertReply(NORMAL)
        chat(says("ok")).assertFriendlyError(HttpStatusCode.TooManyRequests)
        assertEquals(3, provider.calls.size)
    }

    @Test fun exhaustedCareLimiterGivesFallbackWithoutProvider() = testApplication {
        val provider = FakeProvider()
        application { module(deps(provider, careLimiter = RateLimiter(1, hour))) }
        chat(says(heavy)).assertReply(SUPPORT_SUGGESTED)
        val reply = chat(says(heavy)).assertReply(SUPPORT_SUGGESTED)
        assertTrue(reply in Persona.supportFallbackReplies)
        assertEquals(1, provider.calls.size)
    }

    @Test fun providerFailureOnHeavyTurnGivesFallback() = testApplication {
        val provider = FakeProvider(failure = ProviderUnavailable())
        application { module(deps(provider)) }
        assertTrue(chat(says(heavy)).assertReply(SUPPORT_SUGGESTED) in Persona.supportFallbackReplies)
        provider.failure = ProviderRateLimited()
        assertTrue(chat(says("I want to die")).assertReply(SUPPORT_SUGGESTED) in Persona.supportFallbackReplies)
    }

    @Test fun providerFailureOnNormalTurnReturns502() = testApplication {
        application { module(deps(FakeProvider(failure = ProviderUnavailable()))) }
        chat(says("hi")).assertFriendlyError(HttpStatusCode.BadGateway)
    }

    @Test fun blankProviderReplyReturns502() = testApplication {
        application { module(deps(FakeProvider(reply = "  \"\"  "))) }
        chat(says("hi")).assertFriendlyError(HttpStatusCode.BadGateway)
    }

    @Test fun providerRateLimitReturns429() = testApplication {
        application { module(deps(FakeProvider(failure = ProviderRateLimited()))) }
        assertEquals(FriendlyErrors.BUSY, chat(says("hi")).assertFriendlyError(HttpStatusCode.TooManyRequests))
    }

    @Test fun replyMentioningADoctorSuggestsSupport() = testApplication {
        val provider = FakeProvider(reply = "Weeks of no sleep is a lot. Seeing a doctor really is the best option, and I'm still here.")
        application { module(deps(provider)) }
        chat(says("I haven't slept properly in weeks")).assertReply(SUPPORT_SUGGESTED)
        assertEquals(Persona.systemPrompt("SAD", extraCare = false), provider.calls.single().first)
    }

    @Test fun careCheckReadsOnlyTheLastThreeUserMessages() = testApplication {
        val provider = FakeProvider()
        application { module(deps(provider)) }
        fun convo(vararg userTexts: String) = chatJson("SAD", userTexts.flatMap { listOf(Message("user", it), Message("assistant", "I'm here.")) }.dropLast(1))
        chat(convo(heavy, "work", "family")).assertReply(SUPPORT_SUGGESTED)
        chat(convo(heavy, "work", "family", "food")).assertReply(NORMAL)
        assertTrue(Persona.extraCareGuidance in provider.calls[0].first)
        assertFalse(Persona.extraCareGuidance in provider.calls[1].first)
    }
}
