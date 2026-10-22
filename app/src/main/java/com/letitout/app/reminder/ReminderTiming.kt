package com.letitout.app.reminder

import java.time.Duration
import java.time.LocalDateTime

// Time from `now` until the next hour:minute. Never zero: a time that is now or already past today means tomorrow.
internal fun delayUntilNext(hour: Int, minute: Int, now: LocalDateTime): Duration {
    val today = now.toLocalDate().atTime(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
    val next = if (today.isAfter(now)) today else today.plusDays(1)
    return Duration.between(now, next)
}
