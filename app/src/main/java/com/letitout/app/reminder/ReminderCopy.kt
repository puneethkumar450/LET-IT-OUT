package com.letitout.app.reminder

internal data class ReminderMessage(val title: String, val text: String)

// Gentle, no-pressure check-ins. No guilt, no streaks, nothing alarming.
internal val reminderMessages = listOf(
    ReminderMessage("How did today treat you?", "I'm here if you want to talk it out."),
    ReminderMessage("Anything on your mind?", "Sometimes saying it out loud makes it feel lighter. No judgement here."),
    ReminderMessage("Long day?", "Take a few minutes just for you. I'm listening whenever you're ready."),
    ReminderMessage("Just checking in", "How are you really doing today? Big or small, you can let it out."),
    ReminderMessage("Hope today was kind to you", "And if it wasn't, I'd like to hear about it."),
    ReminderMessage("A little space for you", "Want to share how today felt? Only if you feel like it."),
    ReminderMessage("Whatever today brought", "You don't have to keep it all inside. Come say it, in your own words.")
)

// Rotates one message per day without repeating on consecutive days.
internal fun reminderMessageFor(epochDay: Long): ReminderMessage =
    reminderMessages[epochDay.mod(reminderMessages.size)]
