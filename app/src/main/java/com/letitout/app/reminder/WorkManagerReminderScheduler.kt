package com.letitout.app.reminder

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

internal const val REMINDER_WORK_NAME = "daily_check_in_reminder"
internal const val KEY_REMINDER_HOUR = "hour"
internal const val KEY_REMINDER_MINUTE = "minute"

@Singleton
class WorkManagerReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) : ReminderScheduler {
    override fun schedule(hour: Int, minute: Int) {
        ReminderNotifications.ensureChannel(context)
        val delay = delayUntilNext(hour, minute, LocalDateTime.now())
        // The worker reads the chosen time so each run can pin the next one back to it (see CheckInReminderWorker).
        val request = PeriodicWorkRequestBuilder<CheckInReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(KEY_REMINDER_HOUR to hour, KEY_REMINDER_MINUTE to minute))
            .build()
        // CANCEL_AND_REENQUEUE so a newly chosen time replaces the old schedule instead of keeping its period.
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(REMINDER_WORK_NAME, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, request)
    }

    override fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(REMINDER_WORK_NAME)
    }
}
