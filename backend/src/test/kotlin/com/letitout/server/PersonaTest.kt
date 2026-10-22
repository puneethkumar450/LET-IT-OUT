package com.letitout.server

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PersonaTest {
    private val moods = listOf("ANGRY", "SAD", "STRESSED", "ANXIOUS", "LONELY", "HAPPY", "TALK")

    @Test fun promptCombinesBaseAndMoodGuidance() {
        assertEquals(moods.toSet(), Persona.moodGuidance.keys)
        moods.forEach { mood ->
            val prompt = Persona.systemPrompt(mood, extraCare = false)
            assertTrue(prompt.startsWith(Persona.basePrompt))
            assertTrue(Persona.moodGuidance.getValue(mood) in prompt)
            assertFalse(Persona.extraCareGuidance in prompt)
            assertTrue(prompt.endsWith("User-selected mood: $mood. It may not be accurate."))
            moods.filter { it != mood }.forEach { other -> assertFalse(Persona.moodGuidance.getValue(other) in prompt) }
        }
    }

    @Test fun extraCareTextOnlyWhenAsked() {
        val prompt = Persona.systemPrompt("SAD", extraCare = true)
        assertTrue(Persona.extraCareGuidance in prompt)
        assertTrue(prompt.indexOf(Persona.extraCareGuidance) > prompt.indexOf(Persona.moodGuidance.getValue("SAD")))
        assertTrue(prompt.endsWith("User-selected mood: SAD. It may not be accurate."))
    }

    @Test fun unknownMoodFallsBackToJustTalk() {
        assertTrue(Persona.moodGuidance.getValue("TALK") in Persona.systemPrompt("BORED", extraCare = false))
    }

    @Test fun fallbackRepliesAreGentleAndPointToADoctor() {
        assertEquals(3, Persona.supportFallbackReplies.size)
        listOf(
            Persona.supportFallbackReplies to "doctor",
            Persona.hindiFallbackReplies to "डॉक्टर",
            Persona.teluguFallbackReplies to "డాక్టర్"
        ).forEach { (replies, doctor) ->
            assertTrue(replies.size >= 2)
            assertEquals(replies.size, replies.toSet().size)
            replies.forEach { reply ->
                assertNoBannedWords(reply)
                assertTrue("14416" in reply)
                assertTrue("Tele-MANAS" in reply)
                assertTrue(doctor in reply.lowercase(), reply)
                assertTrue(mentionsProfessionalHelp(reply))
                assertFalse(soundsAlarming(reply), reply)
                assertTrue(reply.count { it == '?' } <= 1)
            }
        }
    }

    @Test fun fallbackFollowsThePersonsScript() {
        assertTrue(Persona.fallbackReply("मैं जीना नहीं चाहता") in Persona.hindiFallbackReplies)
        assertTrue(Persona.fallbackReply("నాకు చనిపోవాలని ఉంది") in Persona.teluguFallbackReplies)
        assertTrue(Persona.fallbackReply("main jeena nahi chahta") in Persona.supportFallbackReplies)
        val first = Persona.fallbackReply("मैं जीना नहीं चाहता")
        val second = Persona.fallbackReply("मैं जीना नहीं चाहता", listOf(first))
        assertTrue(second != first && second in Persona.hindiFallbackReplies)
    }

    @Test fun guidanceTextAvoidsBannedWords() {
        Persona.moodGuidance.values.forEach(::assertNoBannedWords)
        assertNoBannedWords(Persona.extraCareGuidance)
        assertNoBannedWords(Persona.rewriteNudge)
        assertNoBannedWords(Persona.neutralReply)
        assertFalse(soundsAlarming(Persona.neutralReply))
    }

    @Test fun fallbackChoiceIsStable() {
        val first = Persona.fallbackReply("I can't do this anymore")
        assertEquals(first, Persona.fallbackReply("I can't do this anymore"))
        assertTrue(first in Persona.supportFallbackReplies)
        val spread = (1..50).map { Persona.fallbackReply("message $it") }.toSet()
        assertTrue(spread.size > 1)
    }

    @Test fun fallbackAvoidsRepeatingRecentReplies() {
        val seed = "ok"
        val first = Persona.fallbackReply(seed)
        val second = Persona.fallbackReply(seed, listOf(first))
        assertTrue(second != first && second in Persona.supportFallbackReplies)
        val third = Persona.fallbackReply(seed, listOf(first, second))
        assertEquals(3, setOf(first, second, third).size)
        // All three already sent: reuse the one sent longest ago.
        assertEquals(first, Persona.fallbackReply(seed, listOf(first, "I'm here.", second, third)))
    }
}
