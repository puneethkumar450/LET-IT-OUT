package com.letitout.server

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CareTest {
    private fun care(vararg messages: String) = needsExtraCare(messages.toList())
    private fun level(vararg messages: String) = careLevel(messages.toList())

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
            "I cut myself in the bathroom again",
            // Clearly heavy phrasings the list used to miss
            "I want my life to end",
            "I'm tired of life, I want my life to end",
            "I don't have a reason to live",
            "there's no reason for me to live",
            "I wish I wasn't alive",
            "I wish I didn't exist",
            "I'd rather not exist",
            "everyone would be happier if I was gone"
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
            "I feel like dying my hair blue",
            "Honestly corporate life isn't worth it",
            "Hostel life isn't worth it yaar",
            "night life isnt worth it in this city",
            "I'm cutting myself a slice of cake",
            "I wish I wasn't here at this wedding",
            "I'd rather not be here at this party",
            "I want everything to be over by Friday"
        ).forEach { assertEquals(CareLevel.NONE, level(it), "should not need extra care: $it") }
    }

    @Test fun ambiguousHeavyPhrasesGetTheSoftTier() {
        listOf(
            "I'm tired of life",
            "I just want everything to end",
            "I wish I wasn't here anymore",
            "I wish I could just disappear forever",
            "sab khatam karna chahta hoon",
            "zindagi se thak gaya hoon",
            "I just want to end it",
            "He cheated on me again. I want to end it.",
            "I can't go on like this",
            "Sometimes I feel life isn't worth it",
            "Honestly, life isn't worth it",
            "I don't want to live, honestly, with my in-laws anymore"
        ).forEach { assertEquals(CareLevel.SOFT, level(it), "should get the soft tier: $it") }
        assertEquals(CareLevel.HARD, level("I can't go on like this", "I want to die"))
        assertEquals(CareLevel.SOFT, level("work was long", "I'm tired of life"))
        assertEquals(CareLevel.NONE, level())
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
            "doctor se baat karna sabse accha rahega",
            // The persona's own "a doctor really is the best option" wording, and close paraphrases
            "A doctor really is the best option for this.",
            "Honestly, a doctor really is the best option here, and I'm still right here.",
            "Going to a doctor really is the best option.",
            "I think a doctor is your best bet right now.",
            "Talking to your doctor would be the best option.",
            "A doctor is the best person to help with panic attacks like these.",
            "I really think a doctor is the best option here.",
            "Sach mein doctor hi best option hai."
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
            "मेरी मम्मी डॉक्टर हैं",
            "A doctor can be so dismissive sometimes, ugh.",
            "So a doctor would have to sign off on it?",
            "With a doctor for a dad, the pressure must be huge.",
            "Becoming a doctor is the best choice you ever made."
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
            "आत्महत्या के ख्याल बहुत भारी होते हैं।",
            "Thoughts of ending your life are so heavy to carry.",
            "Are you having thoughts of taking your own life?",
            "It sounds like you're not wanting to live right now.",
            "Mar jaane ka khayal itna bhaari hai.",
            "ye marne wale khayal bahut heavy hain",
            "मरने के ख्याल बहुत भारी होते हैं।",
            "చనిపోవాలనే ఆలోచనలు చాలా బరువుగా ఉంటాయి.",
            "చనిపోవాలని అనిపిస్తోందా?",
            "You're killing it!",
            "Ugh, that commute would kill anyone's mood.",
            "It's been so hard since your dad died."
        ).forEach { assertTrue(soundsAlarming(it), "should sound alarming: $it") }
        (Persona.supportFallbackReplies + Persona.hindiFallbackReplies + Persona.teluguFallbackReplies + listOf(
            Persona.neutralReply,
            "Stay safe on the drive home.",
            "Ugh, that sounds exhausting.",
            "These waves usually die down after a few minutes.",
            "The urge usually dies down if you give it a little time.",
            "He's a die-hard fan, huh.",
            "Marne do usko, forget him.",
            "Your skills really showed today.",
            "It's time to take your life back, one small step at a time.",
            "मेरा कमरा बहुत छोटा है।",
            "अमर जवान ज्योति देखी?"
        )).forEach { assertFalse(soundsAlarming(it), "should not sound alarming: $it") }
    }

    @Test fun dropsOnlyTheSentencesWithAlarmingWords() {
        assertEquals("Ugh. What's due first?", withoutAlarmingSentences("Ugh. That deadline sounds like a crisis. What's due first?"))
        assertEquals("That's a lot at once. I'm here, what feels hardest?",
            withoutAlarmingSentences("That's a lot at once. You must feel like you're dying!\nI'm here, what feels hardest?"))
        assertNull(withoutAlarmingSentences("That deadline sounds like a crisis."))
        assertNull(withoutAlarmingSentences("Oh no. That sounds like a crisis."))
        assertEquals("Nothing to drop here.", withoutAlarmingSentences("Nothing to drop here."))
    }

    @Test fun spotsDraftsThatBringUpSelfHarm() {
        listOf("Are you having suicidal thoughts?", "Thoughts of ending your life are heavy.", "Is this about self-harm?")
            .forEach { assertTrue(draftNamesSelfHarm(it), it) }
        listOf("There's a crisis at work?", "That sounds like an emergency at home.", "Time to take your life back.", "That deadline is killing you.")
            .forEach { assertFalse(draftNamesSelfHarm(it), it) }
    }
}
