package com.letitout.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
}
