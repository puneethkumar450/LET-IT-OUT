package com.letitout.app.reminder

import android.content.Context
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

class CheckInReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val hour = inputData.getInt(KEY_REMINDER_HOUR, -1)
        val minute = inputData.getInt(KEY_REMINDER_MINUTE, 0)
        runCatching {
            // A run pushed far past the chosen time skips the note rather than arriving late at night.
            val onTime = hour < 0 || isOnTimeForReminder(hour, minute, LocalDateTime.now())
            if (onTime && ReminderNotifications.canPost(applicationContext)) ReminderNotifications.show(applicationContext)
        }
        // Periodic work counts 24 h from when a run finishes, so any lateness would add up day after day.
        // Pin the next run back to the chosen time. Update this worker's own WorkSpec by id: if Settings cancelled or
        // replaced it meanwhile, updateWork does nothing (NOT_APPLIED / missing id) instead of re-creating the reminder.
        if (!isStopped && hour >= 0) runCatching {
            val next = PeriodicWorkRequestBuilder<CheckInReminderWorker>(1, TimeUnit.DAYS)
                .setId(id)
                .setInputData(inputData)
                .setNextScheduleTimeOverride(System.currentTimeMillis() + delayUntilNext(hour, minute, LocalDateTime.now()).toMillis())
                .build()
            WorkManager.getInstance(applicationContext).updateWork(next)
        }
        return Result.success()
    }
}
