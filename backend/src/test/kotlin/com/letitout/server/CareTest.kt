package com.letitout.server

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CareTest {
    private fun care(vararg messages: String) = needsExtraCare(messages.toList())

    @Test fun picksUpHeavySelfDirectedMessages() {
        listOf(
            "I don't want to be here anymore",
            "I don’t want to live anymore.",
            "Honestly everyone would be better off without me",
            "There's no reason to live",
            "I just want to end it all",
            "I keep wanting to hurt myself",
            "I cut myself again last night",
            "Sometimes I want to kill myself",
            "I want to die",
            "I've been thinking about suicide",
            "I've been self-harming again",
            "what's the point of living",
            "main jeena nahi chahta",
            "Main mar jana chahti hoon",
            "khud ko khatam kar dungi",
            "मैं जीना नहीं चाहता",
            "నాకు చనిపోవాలని ఉంది"
        ).forEach { assertTrue(care(it), "should need extra care: $it") }
    }

    @Test fun ignoresEverydayIdiomsAndVenting() {
        listOf(
            "My parents will kill me if I fail",
            "This traffic is killing me",
            "I'm dying to watch it",
            "I could die of embarrassment",
            "I want to die of embarrassment lol",
            "I killed myself laughing at that video",
            "I hurt myself playing cricket",
            "I accidentally cut myself while cooking",
            "I don't want to live in Delhi anymore",
            "I don't want to be here at this party",
            "I'm going to die if mom finds out",
            "The suspense is killing me",
            "My phone died again",
            "This deadline is going to kill me",
            "Papa maar dalenge agar result kharab aaya",
            "Hass hass ke mar gaya",
            "I would never hurt myself, don't worry",
            "My manager upset me at work"
        ).forEach { assertFalse(care(it), "should not need extra care: $it") }
    }

    @Test fun looksAcrossAllRecentMessages() {
        assertTrue(care("I want to die", "anyway", "how was your day"))
        assertFalse(care("work was long", "anyway", "how was your day"))
        assertFalse(care())
    }

    @Test fun recognisesProfessionalHelpMentions() {
        listOf(
            "Seeing a doctor really is the best option, and I'm still here.",
            "Tele-MANAS on 14416 is free, 24x7.",
            "Maybe tele-manas could help",
            "A counselor could help you sort this.",
            "A counsellor might help.",
            "Would a therapist feel okay?",
            "A psychiatrist can look at the sleep side of this.",
            "डॉक्टर से बात करना सबसे अच्छा रहेगा"
        ).forEach { assertTrue(mentionsProfessionalHelp(it), "should mention help: $it") }
        listOf("That sounds exhausting.", "She just finished her doctorate!", "Call me at 144160", "I'm still here.")
            .forEach { assertFalse(mentionsProfessionalHelp(it), "should not mention help: $it") }
    }
}
