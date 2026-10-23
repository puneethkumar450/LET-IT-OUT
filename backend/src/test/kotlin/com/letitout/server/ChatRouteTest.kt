package com.letitout.server

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.testing.*
import io.ktor.utils.io.*
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
        careLimiter: RateLimiter = RateLimiter(30, hour),
        globalLimiter: RateLimiter = RateLimiter(60, 60_000)
    ) = ChatDeps(verifyToken, provider, limiter, careLimiter, globalLimiter)

    private fun chatJson(mood: String, messages: List<Message>, intensity: Int? = null) =
        Json.encodeToString(ChatRequest.serializer(), ChatRequest(mood, messages, intensity))

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

    @Test fun throwingVerifierReturns503NotSignOut() = testApplication {
        val provider = FakeProvider()
        application { module(deps(provider, verifyToken = { throw IllegalStateException("public keys unavailable") })) }
        assertEquals(FriendlyErrors.UNAVAILABLE, chat(says("hi")).assertFriendlyError(HttpStatusCode.ServiceUnavailable))
        assertTrue(provider.calls.isEmpty())
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

    @Test fun bodyOverByteCapWithContentLengthReturns413() = testApplication {
        val provider = FakeProvider()
        application { module(deps(provider)) }
        chat(" ".repeat(300_000)).assertFriendlyError(HttpStatusCode.PayloadTooLarge)
        assertTrue(provider.calls.isEmpty())
    }

    @Test fun streamedBodyOverByteCapReturns413() = testApplication {
        val provider = FakeProvider()
        application { module(deps(provider)) }
        val response = client.post("/v1/chat") {
            header(HttpHeaders.Authorization, "Bearer $goodToken")
            setBody(object : OutgoingContent.WriteChannelContent() {
                override val contentType = ContentType.Application.Json
                override suspend fun writeTo(channel: ByteWriteChannel) {
                    channel.writeFully(ByteArray(300_000) { ' '.code.toByte() })
                }
            })
        }
        response.assertFriendlyError(HttpStatusCode.PayloadTooLarge)
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

    @Test fun intensityReachesTheModelAsAHint() = testApplication {
        val provider = FakeProvider()
        application { module(deps(provider)) }
        chat(chatJson("ANGRY", listOf(Message("user", "My boss yelled again")), intensity = 8)).assertReply(NORMAL)
        val prompt = provider.calls.single().first
        assertEquals(Persona.systemPrompt("ANGRY", extraCare = false, intensity = 8), prompt)
        assertTrue("Self-rated intensity: 8/10. Treat it as a hint, not a measurement." in prompt)
    }

    @Test fun outOfRangeIntensityIsIgnoredNotRefused() = testApplication {
        val provider = FakeProvider()
        application { module(deps(provider)) }
        val bodies = listOf(0, 11, -3, 1000).map { chatJson("STRESSED", listOf(Message("user", "work was long")), intensity = it) } +
            """{"mood":"STRESSED","messages":[{"role":"user","content":"work was long"}],"intensity":null}"""
        bodies.forEach { chat(it).assertReply(NORMAL) }
        assertEquals(bodies.size, provider.calls.size)
        provider.calls.forEach { (prompt, _) ->
            assertEquals(Persona.systemPrompt("STRESSED", extraCare = false), prompt)
            assertFalse("Self-rated intensity" in prompt)
        }
    }

    @Test fun intensityAloneNeverTriggersCare() = testApplication {
        val provider = FakeProvider()
        application { module(deps(provider, limiter = RateLimiter(1, hour))) }
        chat(chatJson("STRESSED", listOf(Message("user", "exam tomorrow")), intensity = 10)).assertReply(NORMAL)
        assertEquals(Persona.systemPrompt("STRESSED", extraCare = false, intensity = 10), provider.calls.single().first)
        // Still an ordinary turn: no care quota to borrow, so the normal limit applies.
        val error = chat(chatJson("STRESSED", listOf(Message("user", "so much to do")), intensity = 10))
            .assertFriendlyError(HttpStatusCode.TooManyRequests)
        assertEquals(FriendlyErrors.TOO_MANY, error)
        assertEquals(1, provider.calls.size)
    }

    @Test fun intensityStaysInCareAndRewritePrompts() = testApplication {
        val provider = FakeProvider()
        application { module(deps(provider)) }
        chat(chatJson("SAD", listOf(Message("user", heavy)), intensity = 9)).assertReply(SUPPORT_SUGGESTED)
        assertEquals(Persona.systemPrompt("SAD", extraCare = true, intensity = 9), provider.calls.single().first)
        provider.queued += "Are you having suicidal thoughts right now?"
        chat(chatJson("SAD", listOf(Message("user", "everything is just too much lately")), intensity = 4)).assertReply(SUPPORT_SUGGESTED)
        assertEquals(Persona.systemPrompt("SAD", extraCare = false, intensity = 4), provider.calls[1].first)
        assertEquals(Persona.systemPrompt("SAD", extraCare = true, intensity = 4) + "\n\n" + Persona.rewriteNudge, provider.calls[2].first)
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

    @Test fun heavyTurnsBorrowNormalQuotaThenGetFallbackWithoutProvider() = testApplication {
        val provider = FakeProvider()
        application { module(deps(provider, limiter = RateLimiter(1, hour), careLimiter = RateLimiter(1, hour))) }
        assertEquals(provider.reply, chat(says(heavy)).assertReply(SUPPORT_SUGGESTED))
        assertEquals(provider.reply, chat(says(heavy)).assertReply(SUPPORT_SUGGESTED))
        val reply = chat(says(heavy)).assertReply(SUPPORT_SUGGESTED)
        assertTrue(reply in Persona.supportFallbackReplies)
        assertEquals(2, provider.calls.size)
        chat(says("thanks")).assertFriendlyError(HttpStatusCode.TooManyRequests)
    }

    @Test fun fallbackDoesNotRepeatTheLastOne() = testApplication {
        application { module(deps(limiter = RateLimiter(0, hour), careLimiter = RateLimiter(0, hour))) }
        val first = chat(says(heavy)).assertReply(SUPPORT_SUGGESTED)
        val convo = chatJson("SAD", listOf(Message("user", heavy), Message("assistant", first), Message("user", heavy)))
        val second = chat(convo).assertReply(SUPPORT_SUGGESTED)
        assertTrue(second in Persona.supportFallbackReplies && second != first)
    }

    @Test fun providerFailureOnHeavyTurnGivesFallback() = testApplication {
        val provider = FakeProvider(failure = ProviderUnavailable())
        application { module(deps(provider)) }
        assertTrue(chat(says(heavy)).assertReply(SUPPORT_SUGGESTED) in Persona.supportFallbackReplies)
        provider.failure = ProviderRateLimited()
        assertTrue(chat(says("I want to die")).assertReply(SUPPORT_SUGGESTED) in Persona.supportFallbackReplies)
    }

    @Test fun failedProviderCallsGiveTheirQuotaBack() = testApplication {
        val provider = FakeProvider(failure = ProviderUnavailable())
        application { module(deps(provider)) }
        repeat(20) { chat(says("hi $it")).assertFriendlyError(HttpStatusCode.BadGateway) }
        provider.failure = ProviderRateLimited()
        chat(says("hi")).assertFriendlyError(HttpStatusCode.TooManyRequests).let { assertEquals(FriendlyErrors.BUSY, it) }
        provider.failure = null
        chat(says("hi again")).assertReply(NORMAL)
    }

    @Test fun globalCapStopsProviderCallsAndReturnsTheSlot() = testApplication {
        var now = 0L
        val provider = FakeProvider()
        application { module(deps(provider, limiter = RateLimiter(2, hour), globalLimiter = RateLimiter(1, 60_000) { now })) }
        chat(says("hi")).assertReply(NORMAL)
        assertEquals(FriendlyErrors.BUSY, chat(says("hi again")).assertFriendlyError(HttpStatusCode.TooManyRequests))
        assertTrue(chat(says(heavy)).assertReply(SUPPORT_SUGGESTED) in Persona.supportFallbackReplies)
        assertEquals(1, provider.calls.size)
        now = 60_000
        chat(says("back now")).assertReply(NORMAL)
        assertEquals(2, provider.calls.size)
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

    @Test fun echoingADoctorInAHappyChatStaysNormal() = testApplication {
        val provider = FakeProvider(reply = "Arre wah! Your sister's a doctor now, that's huge. How are you all celebrating?")
        application { module(deps(provider)) }
        chat(says("My sister just finished MBBS, she's officially a doctor!!", "HAPPY")).assertReply(NORMAL)
        provider.reply = "Hope the doctor's appointment goes okay. What's worrying you most about it?"
        chat(says("I have a doctor's appointment tomorrow", "ANXIOUS")).assertReply(NORMAL)
    }

    @Test fun recentHelplineMentionKeepsExtraCare() = testApplication {
        val provider = FakeProvider()
        application { module(deps(provider, limiter = RateLimiter(0, hour))) }
        val convo = chatJson("SAD", listOf(
            Message("user", "everything feels pointless"),
            Message("assistant", "I'm glad you told me. Tele-MANAS on 14416 is free, 24x7, and I'm still here."),
            Message("user", "ok")
        ))
        chat(convo).assertReply(SUPPORT_SUGGESTED)
        assertEquals(Persona.systemPrompt("SAD", extraCare = true), provider.calls.single().first)
    }

    @Test fun alarmingReplyOnHeavyTurnIsRewrittenWithCare() = testApplication {
        val alarming = "If you are in immediate danger, please call the emergency number 112."
        val provider = FakeProvider(reply = "That's so heavy, and I'm glad you told me. Seeing a doctor really is the best option, and I'm still here.")
        provider.queued += alarming
        application { module(deps(provider)) }
        assertEquals(provider.reply, chat(says(heavy)).assertReply(SUPPORT_SUGGESTED))
        assertEquals(2, provider.calls.size)
        assertEquals(Persona.systemPrompt("SAD", extraCare = true) + "\n\n" + Persona.rewriteNudge, provider.calls[1].first)
        // Both drafts slip and nothing usable is left: a warm fallback, never the flagged words.
        provider.reply = alarming
        val reply = chat(says(heavy)).assertReply(SUPPORT_SUGGESTED)
        assertTrue(reply in Persona.supportFallbackReplies)
        assertNoBannedWords(reply)
        assertEquals(4, provider.calls.size)
    }

    @Test fun heavyTurnInHindiGetsAHindiFallback() = testApplication {
        val provider = FakeProvider(failure = ProviderUnavailable())
        application { module(deps(provider)) }
        assertTrue(chat(says("मैं जीना नहीं चाहता")).assertReply(SUPPORT_SUGGESTED) in Persona.hindiFallbackReplies)
        assertTrue(chat(says("నాకు చనిపోవాలని ఉంది")).assertReply(SUPPORT_SUGGESTED) in Persona.teluguFallbackReplies)
    }

    @Test fun alarmingReplyOnNormalTurnIsRewrittenOnce() = testApplication {
        val provider = FakeProvider(reply = "Ugh, work drama again? What happened this time?")
        provider.queued += "Ugh, another crisis at work? What happened this time?"
        application { module(deps(provider)) }
        assertEquals("Ugh, work drama again? What happened this time?", chat(says("There's a crisis at work", "STRESSED")).assertReply(NORMAL))
        assertEquals(2, provider.calls.size)
        assertEquals(Persona.systemPrompt("STRESSED", extraCare = false) + "\n\n" + Persona.rewriteNudge, provider.calls[1].first)
        // A rewrite that still slips loses the flagged sentence; with nothing left, a short neutral line goes out.
        provider.reply = "That deadline sounds like a crisis."
        assertEquals(Persona.neutralReply, chat(says("deadline tomorrow", "STRESSED")).assertReply(NORMAL))
        assertEquals(4, provider.calls.size)
        provider.reply = "Ugh. That deadline sounds like a crisis. What's due first?"
        assertEquals("Ugh. What's due first?", chat(says("deadline tomorrow", "STRESSED")).assertReply(NORMAL))
        assertEquals(6, provider.calls.size)
    }

    @Test fun alarmingSentenceIsDroppedEvenWithoutTimeForARewrite() = testApplication {
        // The overall cap has room for the first call only, so no rewrite happens.
        val provider = FakeProvider(reply = "Ugh. This deadline is killing you. What's due first?")
        application { module(deps(provider, globalLimiter = RateLimiter(1, 60_000))) }
        assertEquals("Ugh. What's due first?", chat(says("deadline tomorrow", "STRESSED")).assertReply(NORMAL))
        assertEquals(1, provider.calls.size)
    }

    @Test fun draftThatBringsUpSelfHarmTurnsOnCare() = testApplication {
        val provider = FakeProvider(reply = "That sounds so heavy, and I'm really glad you told me. What feels heaviest right now?")
        provider.queued += "Are you having suicidal thoughts right now?"
        application { module(deps(provider)) }
        assertEquals(provider.reply, chat(says("everything is just too much lately")).assertReply(SUPPORT_SUGGESTED))
        assertEquals(Persona.systemPrompt("SAD", extraCare = false), provider.calls[0].first)
        assertEquals(Persona.systemPrompt("SAD", extraCare = true) + "\n\n" + Persona.rewriteNudge, provider.calls[1].first)
    }

    @Test fun softSignalsGetCareGuidanceButTheCardOnlyWithAHelpSuggestion() = testApplication {
        val provider = FakeProvider()
        application { module(deps(provider, limiter = RateLimiter(1, hour), careLimiter = RateLimiter(1, hour))) }
        chat(says("I can't go on like this")).assertReply(NORMAL)
        assertEquals(Persona.systemPrompt("SAD", extraCare = true), provider.calls.single().first)
        provider.reply = "Weeks of this is so much. Honestly, a doctor really is the best option, and I'm still here."
        chat(says("I'm tired of life")).assertReply(SUPPORT_SUGGESTED)
        // Out of quota: like heavy turns, never refused.
        assertTrue(chat(says("I just want to end it")).assertReply(SUPPORT_SUGGESTED) in Persona.supportFallbackReplies)
        assertEquals(2, provider.calls.size)
        chat(says("ok")).assertFriendlyError(HttpStatusCode.TooManyRequests)
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
