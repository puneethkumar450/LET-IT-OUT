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
    private val allLists = listOf(morningMessages, afternoonMessages, reminderMessages)

    @Test
    fun hasAHandfulOfGentleMessages() {
        assertTrue(reminderMessages.size in 5..7)
        assertTrue(morningMessages.size >= 2 && afternoonMessages.size >= 2)
        allLists.flatten().forEach {
            assertTrue(it.title.isNotBlank() && it.text.isNotBlank())
            assertTrue(it.title.length <= 40)
        }
    }

    @Test
    fun avoidsAlarmingOrGuiltyWords() {
        allLists.flatten().forEach {
            val copy = "${it.title} ${it.text}"
            assertFalse("Avoided wording in: $copy", avoided.containsMatchIn(copy))
        }
    }

    @Test
    fun rotatesDailyWithoutRepeatingBackToBack() {
        for (hour in listOf(9, 14, 20, 22)) {
            for (day in 20_000L..20_400L) assertNotEquals(reminderMessageFor(day, hour), reminderMessageFor(day + 1, hour))
        }
        assertEquals(reminderMessages.toSet(), (0L until reminderMessages.size).map { reminderMessageFor(it, 20) }.toSet())
        assertEquals(morningMessages.toSet(), (0L until morningMessages.size).map { reminderMessageFor(it, 9) }.toSet())
        assertTrue(reminderMessageFor(-1L, 20) in reminderMessages)
    }

    @Test
    fun copyFitsTheTimeOfDay() {
        for (day in 20_000L..20_030L) {
            assertTrue(reminderMessageFor(day, 9) in morningMessages)
            assertTrue(reminderMessageFor(day, 14) in afternoonMessages)
            assertTrue(reminderMessageFor(day, 20) in reminderMessages)
            assertTrue(reminderMessageFor(day, 22) in reminderMessages)
        }
        // Evening copy ("How did today treat you?", "Long day?") never shows up in the morning or afternoon.
        assertTrue(reminderMessages.none { it in morningMessages || it in afternoonMessages })
    }
}
