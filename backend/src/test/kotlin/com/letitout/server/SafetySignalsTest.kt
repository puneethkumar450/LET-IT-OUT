package com.letitout.server

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SafetySignalsTest {
    @Test fun checksExplicitRiskPhrases() {
        assertTrue(mayNeedImmediateHelp("I want to die"))
        assertTrue(mayNeedImmediateHelp("I will kill myself"))
        assertTrue(mayNeedImmediateHelp("Honestly everyone would be better off without me"))
        assertTrue(mayNeedImmediateHelp("I don’t want to live anymore"))
        assertTrue(mayNeedImmediateHelp("main jeena nahi chahta"))
    }

    @Test fun doesNotLabelOrdinaryWorkplaceComplaints() {
        assertFalse(mayNeedImmediateHelp("My manager upset me at work"))
        assertFalse(mayNeedImmediateHelp("I'm so angry I could scream"))
    }

    @Test fun classifierAnswerParsing() {
        assertTrue(classifierFlagsSelfHarm("1"))
        assertTrue(classifierFlagsSelfHarm(" 1\n"))
        assertFalse(classifierFlagsSelfHarm("0"))
        assertFalse(classifierFlagsSelfHarm(""))
        assertFalse(classifierFlagsSelfHarm("I can't help with that"))
    }

    @Test fun classifierIsSkippedWithoutKeyOrWhenTurnedOff() = runBlocking {
        val request = ChatRequest("SAD", listOf(Message("user", "I can't see the point anymore")))
        assertFalse(groqSafetyCheck(request, ServerConfig.fromEnvironment(emptyMap())))
        val off = ServerConfig.fromEnvironment(mapOf("GROQ_API_KEY" to "test", "GROQ_SAFETY_MODEL" to "off"))
        assertNull(off.groqSafetyModel)
        assertFalse(groqSafetyCheck(request, off))
        assertEquals(ServerConfig.DEFAULT_SAFETY_MODEL, ServerConfig.fromEnvironment(emptyMap()).groqSafetyModel)
    }
}
