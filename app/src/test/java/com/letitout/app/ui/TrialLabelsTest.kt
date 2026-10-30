package com.letitout.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class TrialLabelsTest {
    @Test fun chatTimerRoundsUpToWholeMinutes() {
        assertEquals("15 min left", minutesLeftLabel(15 * 60_000L))
        assertEquals("13 min left", minutesLeftLabel(12 * 60_000L + 1))
        assertEquals("2 min left", minutesLeftLabel(CHAT_WARNING_MS))
        assertEquals("1 min left", minutesLeftLabel(59_000L))
        assertEquals("1 min left", minutesLeftLabel(0L))
    }

    @Test fun liveClockShowsMinutesAndSeconds() {
        assertEquals("3:00", liveClockLabel(180_000L))
        assertEquals("0:28", liveClockLabel(27_400L))
        assertEquals("0:00", liveClockLabel(-500L))
    }

    @Test fun resetTimeIsApproximate() {
        val now = 1_000_000_000L
        assertEquals("in about 4 hours", resetInLabel(now + 4 * 3_600_000L + 10 * 60_000L, now))
        assertEquals("in about an hour", resetInLabel(now + 55 * 60_000L, now))
        assertEquals("in about 40 minutes", resetInLabel(now + 38 * 60_000L, now))
        assertEquals("in a few minutes", resetInLabel(now + 3 * 60_000L, now))
    }

    @Test fun nextLiveDateIsShort() {
        val calendar = Calendar.getInstance(TimeZone.getDefault()).apply { set(2026, Calendar.NOVEMBER, 5, 10, 0) }
        assertEquals("Thu, 5 Nov", shortDateLabel(calendar.timeInMillis, Locale.UK))
    }
}
