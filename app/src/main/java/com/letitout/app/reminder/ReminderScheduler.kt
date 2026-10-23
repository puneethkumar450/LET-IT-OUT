package com.letitout.app.reminder

interface ReminderScheduler {
    fun schedule(hour: Int, minute: Int)
    fun cancel()
}
