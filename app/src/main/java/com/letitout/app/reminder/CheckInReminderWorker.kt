package com.letitout.app.reminder

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class CheckInReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        runCatching {
            if (ReminderNotifications.canPost(applicationContext)) ReminderNotifications.show(applicationContext)
        }
        return Result.success()
    }
}
