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
            "నాకు చనిపోవాలని ఉంది",
            // Common phrasings the first list missed
            "I feel like dying",
            "Life isn't worth living",
            "I wish I'd never been born",
            "I want to end everything",
            "I just want to end it",
            "I want to just die",
            "I don't want to live, I'm so tired",
            "what is the point of living",
            "I don't see the point of living anymore",
            "I can't live like this anymore",
            "I can't go on anymore",
            "I don't want to go on with my life anymore",
            "I don't want to wake up tomorrow",
            "I'm done with life",
            "I want to die of this pain",
            "I want to overdose",
            "I'm thinking about an overdose",
            "I overdosed on my sleeping pills",
            "I took a bunch of sleeping pills",
            "thinking of taking all my sleeping pills",
            "I'm thinking of taking all my pills",
            "kms",
            "i wanna kms",
            "marne ka mann karta hai",
            "marne ka dil kar raha hai",
            "mujhe mar jana hai",
            "zindagi khatam karna chahta hoon",
            "zeher kha lunga",
            "phansi laga lunga",
            "जीना नही चाहता",
            "मरने का मन करता है",
            "chanipovalanipistundi",
            "naaku chaavaalani undi",
            "చావాలని ఉంది",
            // Idiom exceptions must not hide these
            "I want to kill myself to make the pain stop",
            "I want to kill myself to get away from all this",
            "I feel like killing myself to make it stop",
            "I almost killed myself over it last year",
            "I keep cutting myself off and I want to hurt myself",
            "I cut myself in the bathroom again"
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
            "I would never, ever hurt myself",
            "My manager upset me at work",
            "I'm trying to cut myself some slack",
            "I've cut myself off from my friends",
            "I fell and hurt myself",
            "I hurt myself at cricket practice",
            "I cut myself on paper",
            "I could kill myself for forgetting her birthday",
            "I'm killing myself studying for NEET",
            "I'm killing myself trying to finish this project",
            "I killed myself studying and still failed",
            "I don't want to go on anymore with this project",
            "There's no point going on with this course",
            "I can't go on with this project anymore",
            "I need to take my pills",
            "I take my sleeping pills at 10",
            "I take a lot of tablets for my BP",
            "I have to take all my tablets after food",
            "Life is worth living after all",
            "I want to end it with him",
            "He keeps lying. I'm going to end it.",
            "I'm ready to end it and move on",
            "I don't want to wake up early tomorrow",
            "office is 12 kms away",
            "I ran 5 kms today",
            "my account is in OD",
            "I want to overdose on chocolate",
            "I overdosed on coffee before the exam",
            "cuteness overdose",
            "I was reading about overdose symptoms",
            "koi fanda lagate hain",
            "Aaj raat tak sab khatam kar dunga",
            "Meri life khatam ho gayi yaar",
            "I want to die of happiness",
            "I feel like dying my hair blue"
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
            "डॉक्टर से बात करना सबसे अच्छा रहेगा",
            "Honestly, it might really help to talk to a doctor about this.",
            "Have you thought about seeing a doctor?",
            "A good doctor could really help with the sleep.",
            "Doctors can help with exactly this.",
            "It might be worth getting checked by a doctor.",
            "Getting a doctor's help would make a real difference.",
            "The best person to help you through it is a doctor.",
            "Ek baar doctor se mil lo, sach mein best rahega.",
            "doctor se baat karna sabse accha rahega"
        ).forEach { assertTrue(mentionsProfessionalHelp(it), "should mention help: $it") }
        listOf(
            "That sounds exhausting.", "She just finished her doctorate!", "Call me at 144160", "I'm still here.",
            // Echoes of the person's own doctor, therapist or career plans are not a recommendation
            "Arre wah! Your sister's a doctor now, that's huge.",
            "That's a relief that your doctor said the reports are fine!",
            "How did the career counselling go?",
            "You don't need a doctor for this, it's just a rough day.",
            "Your therapist sounds great.",
            "Night shifts as a doctor are brutal.",
            "As a doctor you could help so many people.",
            "Becoming a doctor has been your dream for so long, no wonder NEET feels this heavy.",
            "Becoming a doctor will take time, but you're on your way.",
            "What did the doctor say when you went in?",
            "Being a therapist means holding everyone's pain all day.",
            "You've been to the counsellor at college before, right?",
            "The best way to become a doctor is one step at a time.",
            "मेरी मम्मी डॉक्टर हैं"
        ).forEach { assertFalse(mentionsProfessionalHelp(it), "should not mention help: $it") }
    }

    @Test fun recognisesHelplineMentions() {
        assertTrue(mentionsHelpline("Tele-MANAS on 14416 is free, 24x7."))
        assertTrue(mentionsHelpline("You can call 14416 anytime."))
        assertTrue(mentionsHelpline("telemanas is there too"))
        assertFalse(mentionsHelpline("Seeing a doctor really is the best option."))
        assertFalse(mentionsHelpline("Call me at 144160"))
    }

    @Test fun flagsAlarmingReplyWords() {
        listOf(
            "If you are in immediate danger, call emergency services at 112.",
            "You said you want to kill yourself, and that's so heavy.",
            "That sounds like a crisis.",
            "Thinking about suicide is exhausting.",
            "His death left such a hole.",
            "Are you safe right now?",
            "आत्महत्या के ख्याल बहुत भारी होते हैं।"
        ).forEach { assertTrue(soundsAlarming(it), "should sound alarming: $it") }
        (Persona.supportFallbackReplies + listOf("You're killing it!", "Stay safe on the drive home.", "Ugh, that sounds exhausting."))
            .forEach { assertFalse(soundsAlarming(it), "should not sound alarming: $it") }
    }
}
