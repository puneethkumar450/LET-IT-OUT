package com.letitout.app.reminder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderCopyTest {
    private val avoided = Regex(
        "\\b(crisis|emergency|suicid\\w*|kill\\w*|die|dying|death|danger\\w*|life-threatening|are you safe|streak\\w*|" +
            "miss(ed)? you|haven't|forgot\\w*|don't forget)\\b",
        RegexOption.IGNORE_CASE
    )

    @Test
    fun hasAHandfulOfGentleMessages() {
        assertTrue(reminderMessages.size in 5..7)
        reminderMessages.forEach {
            assertTrue(it.title.isNotBlank() && it.text.isNotBlank())
            assertTrue(it.title.length <= 40)
        }
    }

    @Test
    fun avoidsAlarmingOrGuiltyWords() {
        reminderMessages.forEach {
            val copy = "${it.title} ${it.text}"
            assertFalse("Avoided wording in: $copy", avoided.containsMatchIn(copy))
        }
    }

    @Test
    fun rotatesDailyWithoutRepeatingBackToBack() {
        for (day in 20_000L..20_400L) assertNotEquals(reminderMessageFor(day), reminderMessageFor(day + 1))
        assertEquals(reminderMessages.toSet(), (0L until reminderMessages.size).map { reminderMessageFor(it) }.toSet())
        assertTrue(reminderMessageFor(-1L) in reminderMessages)
    }
}
