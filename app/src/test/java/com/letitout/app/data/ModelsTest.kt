package com.letitout.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
            banned.forEach { assertFalse("${mood.name} prompt contains \"$it\"", prompt.contains(it)) }
        }
    }

    @Test fun defaultsMatchContract() {
        assertEquals(ReminderSettings(false, 20, 0), ReminderSettings())
        assertEquals(CheckInStats(0, 0), CheckInStats())
        assertEquals("NORMAL", ChatResponse("hi").safetyState)
    }

    @Test fun chatRequestSendsTheLastTwelveMessagesEndingWithThePerson() {
        val conversation = (1..13).map { ChatMessage(it.toLong(), "message $it", fromUser = it % 2 == 1) }
        val request = chatRequestFor(Mood.SAD, conversation)
        assertEquals("SAD", request.mood)
        assertEquals(12, request.messages.size)
        assertEquals(ApiMessage("assistant", "message 2"), request.messages.first())
        assertEquals(ApiMessage("user", "message 13"), request.messages.last())
    }

    @Test fun shortChatStartsWithTheOpeningQuestionAndTrimsMessages() {
        val conversation = listOf(ChatMessage(1, "  hi  ", true), ChatMessage(2, "   ", false), ChatMessage(3, "x".repeat(2500), true))
        assertEquals(
            listOf(ApiMessage("assistant", Mood.TALK.prompt), ApiMessage("user", "hi"), ApiMessage("user", "x".repeat(2000))),
            chatRequestFor(Mood.TALK, conversation).messages
        )
    }

    @Test fun responseBecomesAReplyWithTheSupportFlag() {
        assertEquals(ChatReply("Hey.", true), ChatResponse("  Hey. ", "SUPPORT_SUGGESTED").toChatReply())
        assertEquals(ChatReply("Hey.", false), ChatResponse("Hey.").toChatReply())
        assertEquals(ChatReply("Hey.", false), ChatResponse("Hey.", "SOMETHING_NEW").toChatReply())
        assertThrows(FriendlyException::class.java) { ChatResponse("   ").toChatReply() }
    }
}
