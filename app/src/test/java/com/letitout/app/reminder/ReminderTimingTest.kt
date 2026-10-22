package com.letitout.app.reminder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDateTime

class ReminderTimingTest {
    private fun at(text: String) = LocalDateTime.parse(text)

    @Test
    fun laterTodayWaitsUntilThen() {
        assertEquals(Duration.ofHours(10), delayUntilNext(20, 0, at("2026-10-22T10:00:00")))
    }

    @Test
    fun exactlyNowMovesToTomorrow() {
        assertEquals(Duration.ofDays(1), delayUntilNext(20, 0, at("2026-10-22T20:00:00")))
    }

    @Test
    fun justPassedMovesToTomorrowAndStaysPositive() {
        val delay = delayUntilNext(20, 0, at("2026-10-22T20:00:00.000000001"))
        assertEquals(Duration.ofDays(1).minusNanos(1), delay)
    }

    @Test
    fun earlierTodayMovesToTomorrow() {
        assertEquals(Duration.ofHours(22).plusMinutes(30), delayUntilNext(20, 0, at("2026-10-22T21:30:00")))
    }

    @Test
    fun secondsBeforeTheTimeAreRespected() {
        assertEquals(Duration.ofSeconds(30), delayUntilNext(9, 15, at("2026-10-22T09:14:30")))
    }

    @Test
    fun midnightTargetLateInTheEvening() {
        assertEquals(Duration.ofSeconds(30), delayUntilNext(0, 0, at("2026-10-22T23:59:30")))
    }

    @Test
    fun rollsOverMonthAndYear() {
        assertEquals(Duration.ofHours(10), delayUntilNext(8, 0, at("2026-12-31T22:00:00")))
    }

    @Test
    fun rollsIntoLeapDay() {
        assertEquals(Duration.ofHours(8), delayUntilNext(7, 0, at("2028-02-28T23:00:00")))
    }

    @Test
    fun outOfRangeValuesAreClamped() {
        assertEquals(Duration.ofHours(13).plusMinutes(59), delayUntilNext(25, 99, at("2026-10-22T10:00:00")))
        assertEquals(Duration.ofHours(14), delayUntilNext(-3, -5, at("2026-10-22T10:00:00")))
    }

    @Test
    fun alwaysPositiveAndAtMostOneDay() {
        val start = at("2026-10-22T00:00:00")
        for (step in 0 until 24 * 4) {
            val now = start.plusMinutes(step * 15L).plusSeconds(7)
            for ((hour, minute) in listOf(0 to 0, 9 to 0, 14 to 0, 20 to 0, 22 to 0, 23 to 59)) {
                val delay = delayUntilNext(hour, minute, now)
                assertTrue("$now -> $hour:$minute gave $delay", !delay.isNegative && !delay.isZero)
                assertTrue("$now -> $hour:$minute gave $delay", delay <= Duration.ofDays(1))
                val fire = now.plus(delay)
                assertEquals(hour, fire.hour)
                assertEquals(minute, fire.minute)
            }
        }
    }
}
