package com.letitout.app.reminder

internal data class ReminderMessage(val title: String, val text: String)

// Gentle, no-pressure check-ins. No guilt, no streaks, nothing alarming.
internal val morningMessages = listOf(
    ReminderMessage("Good morning", "Whatever today holds, I'm here if anything's on your mind."),
    ReminderMessage("A quiet moment before the day", "Anything you'd like to get off your chest first? No pressure."),
    ReminderMessage("Morning check-in", "How are you feeling as the day starts? If something's sitting heavy, you can talk it out here.")
)

internal val afternoonMessages = listOf(
    ReminderMessage("How's your day going?", "If it's been a lot so far, take a few minutes. I'm listening."),
    ReminderMessage("A little pause for you", "Halfway through the day. Want to share how it's feeling?"),
    ReminderMessage("Checking in on your day", "Big or small, whatever's on your mind, you can let it out here.")
)

internal val reminderMessages = listOf(
    ReminderMessage("How did today treat you?", "I'm here if you want to talk it out."),
    ReminderMessage("Anything on your mind?", "Sometimes saying it out loud makes it feel lighter. No judgement here."),
    ReminderMessage("Long day?", "Take a few minutes just for you. I'm listening whenever you're ready."),
    ReminderMessage("Just checking in", "How are you really doing today? Big or small, you can let it out."),
    ReminderMessage("Hope today was kind to you", "And if it wasn't, I'd like to hear about it."),
    ReminderMessage("A little space for you", "Want to share how today felt? Only if you feel like it."),
    ReminderMessage("Whatever today brought", "You don't have to keep it all inside. Come say it, in your own words.")
)

// Picks copy that fits the time of day, rotating one message per day without repeating on consecutive days.
internal fun reminderMessageFor(epochDay: Long, hour: Int): ReminderMessage {
    val messages = when (hour) {
        in 0..11 -> morningMessages
        in 12..16 -> afternoonMessages
        else -> reminderMessages
    }
    return messages[epochDay.mod(messages.size)]
}
