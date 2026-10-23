package com.letitout.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelsTest {
    @Test fun moodOrderIsUnchanged() {
        assertEquals(listOf("ANGRY", "SAD", "STRESSED", "ANXIOUS", "LONELY", "HAPPY", "TALK"), Mood.entries.map { it.name })
    }

    @Test fun openingPromptsSoundLikeAFriend() {
        val banned = listOf("crisis", "emergency", "suicid", "kill", "die", "death", "danger", "are you safe", "as an ai", "assist")
        Mood.entries.forEach { mood ->
            val prompt = mood.prompt.lowercase()
            assertTrue("${mood.name} prompt is too long", prompt.length in 10..120)
            listOf(prompt, mood.heading.lowercase(), mood.reassurance.lowercase()).forEach { text ->
                banned.forEach { assertFalse("${mood.name} text contains \"$it\": $text", text.contains(it)) }
            }
        }
    }

    @Test fun defaultsMatchContract() {
        assertEquals(ReminderSettings(false, 20, 0), ReminderSettings())
        assertEquals(CheckInStats(0, 0), CheckInStats())
        assertEquals(AppSettings(readAloud = true, voiceInput = true, followMoodTheme = true, onboarded = false), AppSettings())
        assertEquals("NORMAL", ChatResponse("hi").safetyState)
        assertNull(ChatRequest("TALK", emptyList()).intensity)
        assertFalse(ChatMessage(1, "hi", true).supportResources)
    }

    @Test fun chatRequestSendsTheLastTwelveMessagesEndingWithThePerson() {
        val conversation = (1..13).map { ChatMessage(it.toLong(), "message $it", fromUser = it % 2 == 1) }
        val request = chatRequestFor(Mood.SAD, conversation)
        assertEquals("SAD", request.mood)
        assertEquals(12, request.messages.size)
        assertEquals(ApiMessage("assistant", "message 2"), request.messages.first())
        assertEquals(ApiMessage("user", "message 13"), request.messages.last())
        assertNull(request.intensity)
    }

    @Test fun shortChatStartsWithTheOpeningQuestionAndTrimsMessages() {
        val conversation = listOf(ChatMessage(1, "  hi  ", true), ChatMessage(2, "   ", false), ChatMessage(3, "x".repeat(2500), true))
        assertEquals(
            listOf(ApiMessage("assistant", Mood.TALK.prompt), ApiMessage("user", "hi"), ApiMessage("user", "x".repeat(2000))),
            chatRequestFor(Mood.TALK, conversation).messages
        )
    }

    @Test fun chatRequestCarriesTheSelfRatedIntensityWhenItIsInRange() {
        val conversation = listOf(ChatMessage(1, "my manager yelled at me", true))
        assertEquals(8, chatRequestFor(Mood.ANGRY, conversation, 8).intensity)
        assertEquals(1, chatRequestFor(Mood.ANGRY, conversation, 1).intensity)
        assertEquals(10, chatRequestFor(Mood.ANGRY, conversation, 10).intensity)
        assertNull(chatRequestFor(Mood.ANGRY, conversation, null).intensity)
        assertNull(chatRequestFor(Mood.ANGRY, conversation, 0).intensity)
        assertNull(chatRequestFor(Mood.ANGRY, conversation, 11).intensity)
    }

    @Test fun responseBecomesAReplyWithTheSupportFlag() {
        assertEquals(ChatReply("Hey.", true), ChatResponse("  Hey. ", "SUPPORT_SUGGESTED").toChatReply())
        assertEquals(ChatReply("Hey.", false), ChatResponse("Hey.").toChatReply())
        assertEquals(ChatReply("Hey.", false), ChatResponse("Hey.", "SOMETHING_NEW").toChatReply())
        assertThrows(FriendlyException::class.java) { ChatResponse("   ").toChatReply() }
    }

    @Test fun reportReasonsSendStableSlugs() {
        assertEquals(listOf("hurtful", "unsafe", "wrong", "other"), ReportReason.entries.map { it.slug })
        assertTrue(ReportReason.entries.all { it.label.isNotBlank() })
    }
}
