package com.letitout.app.reminder

import java.time.Duration
import java.time.LocalDateTime

// Time from `now` until the next hour:minute. Never zero: a time that is now or already past today means tomorrow.
internal fun delayUntilNext(hour: Int, minute: Int, now: LocalDateTime): Duration {
    val today = now.toLocalDate().atTime(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
    val next = if (today.isAfter(now)) today else today.plusDays(1)
    return Duration.between(now, next)
}

// How late a check-in may still arrive. Later than this (Doze, battery saver) it is skipped instead of landing at night.
internal val REMINDER_LATENESS_LIMIT: Duration = Duration.ofHours(2)

// True when `now` is at today's hour:minute or up to REMINDER_LATENESS_LIMIT after it.
internal fun isOnTimeForReminder(hour: Int, minute: Int, now: LocalDateTime): Boolean {
    val late = Duration.between(now.toLocalDate().atTime(hour.coerceIn(0, 23), minute.coerceIn(0, 59)), now)
    return !late.isNegative && late <= REMINDER_LATENESS_LIMIT
}
