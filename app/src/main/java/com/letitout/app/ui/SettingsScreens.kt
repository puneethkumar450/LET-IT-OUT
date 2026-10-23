package com.letitout.app.ui

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.letitout.app.BuildConfig
import com.letitout.app.data.Mood
import com.letitout.app.data.palette
import com.letitout.app.reminder.ReminderNotifications

private enum class SettingsDialog { THEME, CLEAR_DATA, DELETE_ACCOUNT }

private val reminderTimes = listOf("9 AM" to 9, "2 PM" to 14, "8 PM" to 20, "10 PM" to 22)
private const val NOTIFICATIONS_OFF =
    "Notifications are turned off for Let It Out, so the check-in can't show up yet. You can turn them on in your phone settings."
private const val NOTIFICATIONS_DECLINED =
    "That's okay. If you'd like a daily check-in later, you can allow notifications for Let It Out in your phone settings."

@Composable
internal fun SettingsScreen(vm: AppViewModel, onBack: () -> Unit, onPrivacy: () -> Unit, onSignIn: () -> Unit, onSignedOut: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var status by remember { mutableStateOf<String?>(null) }
    var dialog by remember { mutableStateOf<SettingsDialog?>(null) }
    var note by remember { mutableStateOf<String?>(null) }
    // Re-checked on every return to the screen, so turning notifications off (or back on) in phone settings shows up here.
    var canPost by remember { mutableStateOf(ReminderNotifications.canPost(context)) }
    LifecycleResumeEffect(Unit) {
        canPost = ReminderNotifications.canPost(context)
        if (canPost && note == NOTIFICATIONS_DECLINED) note = null
        onPauseOrDispose { }
    }
    val enableReminder: () -> Unit = {
        vm.setReminder(true, state.reminder.hour, state.reminder.minute)
        canPost = ReminderNotifications.canPost(context)
        note = null // the "notifications are off" line follows canPost on its own
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) enableReminder() else note = NOTIFICATIONS_DECLINED
    }
    val onReminderToggle: (Boolean) -> Unit = { on ->
        if (!on) {
            vm.setReminder(false, state.reminder.hour, state.reminder.minute)
            note = null
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !ReminderNotifications.canPost(context)) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            enableReminder()
        }
    }
    val shownNote = note ?: if (state.reminder.enabled && !canPost) NOTIFICATIONS_OFF else null
    ScrollScreen(AppColors.Background, PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 12.dp)) {
        PageHeader("Settings", onBack)
        Spacer(Modifier.height(16.dp))
        AccountCard(state, onSignIn)
        Spacer(Modifier.height(16.dp))
        SettingsGroup("Appearance") {
            SettingsRow("App theme", { dialog = SettingsDialog.THEME }, value = if (state.settings.followMoodTheme) "Follow my mood" else "Always calm")
        }
        Spacer(Modifier.height(16.dp))
        SettingsGroup("Speech") {
            ToggleRow("Read replies aloud", state.settings.readAloud, vm::setReadAloud)
            RowDivider()
            ToggleRow("Voice input (push to talk)", state.settings.voiceInput, vm::setVoiceInput)
        }
        Spacer(Modifier.height(16.dp))
        SettingsGroup("Daily check-in") {
            ToggleRow("Gentle daily note", state.reminder.enabled, onReminderToggle, detail = "Once a day, in case you'd like to talk.")
            RowDivider()
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Remind me at", color = AppColors.Muted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    reminderTimes.forEach { (label, hour) ->
                        // Picking a time while the check-in is on reschedules it; while off it just remembers the choice.
                        ReminderTimeChip(label, state.reminder.hour == hour && state.reminder.minute == 0, Modifier.weight(1f).fillMaxHeight()) {
                            vm.setReminder(state.reminder.enabled, hour, 0)
                        }
                    }
                }
                shownNote?.let { Text(it, color = AppColors.Muted, fontSize = 13.sp, lineHeight = 19.sp) }
            }
            RowDivider()
            SettingsRow("Clear check-in history", chevron = false, onClick = {
                vm.clearCheckIns()
                Toast.makeText(context, "Check-in history cleared", Toast.LENGTH_SHORT).show()
            })
        }
        Spacer(Modifier.height(16.dp))
        SettingsGroup("Privacy & data") {
            SettingsRow("Privacy policy", onPrivacy)
            RowDivider()
            SettingsRow("Terms of use", onPrivacy)
            RowDivider()
            SettingsRow("Delete conversation data on this device", { dialog = SettingsDialog.CLEAR_DATA })
        }
        if (state.signedIn) {
            Spacer(Modifier.height(16.dp))
            SettingsGroup("Account") {
                // Check-ins and the daily check-in aren't tied to an account, so signing out clears them for whoever comes next.
                SettingsRow(
                    "Sign out", chevron = false, detail = "Clears your check-in answers and the daily reminder on this phone",
                    onClick = { status = null; vm.signOut(onDone = onSignedOut) }
                )
                RowDivider()
                SettingsRow("Delete account and data", { dialog = SettingsDialog.DELETE_ACCOUNT }, chevron = false, color = AppColors.Error, bold = true)
            }
        }
        status?.let { Spacer(Modifier.height(14.dp)); Text(it, color = AppColors.Error, fontSize = 14.sp, lineHeight = 20.sp) }
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            LinkButton("About Let It Out", AppColors.Primary, onPrivacy)
            Text("Version ${BuildConfig.VERSION_NAME}", Modifier.padding(end = 6.dp), color = AppColors.Muted, fontSize = 13.sp)
        }
    }
    val close = { dialog = null }
    when (dialog) {
        SettingsDialog.THEME -> AlertDialog(
            onDismissRequest = close,
            title = { Text("App theme") },
            text = {
                Column(Modifier.selectableGroup()) {
                    ThemeOption("Follow my mood", "Colours change with the mood you pick", state.settings.followMoodTheme) { vm.setFollowMoodTheme(true); close() }
                    ThemeOption("Always calm", "Keep the lavender theme everywhere", !state.settings.followMoodTheme) { vm.setFollowMoodTheme(false); close() }
                }
            },
            confirmButton = { TextButton(onClick = close) { Text("Done") } }
        )
        SettingsDialog.CLEAR_DATA -> AlertDialog(
            onDismissRequest = close,
            title = { Text("Delete conversation data?") },
            text = {
                Text(
                    "This clears the current conversation, your saved mood and your \"How are you feeling now?\" answers on this device. " +
                        "Your settings and daily check-in reminder stay as they are."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    close(); vm.clearDeviceData()
                    Toast.makeText(context, "Conversation data deleted", Toast.LENGTH_SHORT).show()
                }) { Text("Delete", color = AppColors.Error) }
            },
            dismissButton = { TextButton(onClick = close) { Text("Cancel") } }
        )
        SettingsDialog.DELETE_ACCOUNT -> AlertDialog(
            onDismissRequest = close,
            title = { Text("Delete your account?") },
            text = {
                Text(
                    "This permanently removes your sign-in account, clears the conversation and your \"How are you feeling now?\" answers " +
                        "on this device, and turns off the daily check-in reminder. Conversations aren't stored on our server."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    close(); status = null
                    vm.deleteAccount(onDone = onSignedOut, onError = { status = it })
                }) { Text("Delete", color = AppColors.Error) }
            },
            dismissButton = { TextButton(onClick = close) { Text("Cancel") } }
        )
        null -> Unit
    }
}

@Composable
private fun AccountCard(state: AppViewModel.UiState, onSignIn: () -> Unit) {
    val (title, detail, action) = when {
        !state.signedIn -> Triple("Not signed in", "Sign in or continue as a guest to start talking", "Sign in")
        state.guest -> Triple("Guest", "Link Google to keep your account across devices", "Link")
        else -> Triple(state.userName ?: "Your account", state.userEmail ?: "Signed in with Google", null)
    }
    val shape = RoundedCornerShape(22.dp)
    Row(
        Modifier.fillMaxWidth().softShadow(AppColors.Ink, shape, 3.dp).background(AppColors.Surface, shape).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(Modifier.size(48.dp).background(AppColors.Lavender, CircleShape), contentAlignment = Alignment.Center) {
            if (state.signedIn) Text(title.take(1).uppercase(), fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = AppColors.Primary)
            else Icon(AppIcons.Heart, contentDescription = null, tint = AppColors.Primary, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(detail, color = AppColors.Muted, fontSize = 13.sp, lineHeight = 18.sp)
        }
        action?.let { SmallPillButton(it, onSignIn, container = AppColors.Primary, content = Color.White) }
    }
}

@Composable
private fun ReminderTimeChip(label: String, selected: Boolean, modifier: Modifier, onSelect: () -> Unit) {
    Box(
        modifier.heightIn(min = 48.dp).clip(CircleShape).background(if (selected) AppColors.Primary else AppColors.Lavender)
            .selectable(selected, role = Role.RadioButton, onClick = onSelect).padding(horizontal = 4.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) Color.White else AppColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ThemeOption(title: String, detail: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).selectable(selected, role = Role.RadioButton, onClick = onSelect).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RadioButton(selected = selected, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = AppColors.Primary))
        Column {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppColors.Ink)
            Text(detail, fontSize = 13.sp, color = AppColors.Muted)
        }
    }
}

private class HelpTopic(val icon: ImageVector, val mood: Mood, val title: String, val summary: String, val detail: String)

private val helpTopics = listOf(
    HelpTopic(AppIcons.Chat, Mood.TALK, "Not a therapist", "An AI companion, not medical care or\ndiagnosis",
        "Let It Out is an AI listening companion. It can't diagnose or treat anything, and it isn't a therapist or a doctor. " +
            "It's here to listen, and it can sometimes get things wrong."),
    HelpTopic(AppIcons.Sync, Mood.SAD, "How your messages are processed", "Sent to our server and an AI provider to\nreply",
        "Your messages are sent to our server and an AI provider so a reply can be written. Don't share anything you aren't comfortable " +
            "sending to those services. If you use the mic, your phone's speech service (often Google) turns your voice into text; " +
            "Let It Out doesn't save the audio."),
    HelpTopic(AppIcons.Storage, Mood.STRESSED, "What we store", "Mood, settings and check-ins on your phone —\nnot conversations",
        "Your selected mood, settings, \"How are you feeling now?\" answers and daily check-in time stay on this phone. Check-ins and " +
            "the reminder are cleared when you sign out or delete your account, and you can clear check-ins anytime in Settings. " +
            "We don't save chat history on our server, and a conversation is cleared when you close the app."),
    HelpTopic(AppIcons.Trash, Mood.ANGRY, "Deleting your data", "Clear a chat, wipe this device, or delete\nyour account",
        "Start a new conversation from the ⋯ menu in a chat, clear conversation data and check-ins on this device in Settings, " +
            "or delete your account in Settings › Account."),
    HelpTopic(AppIcons.Flag, Mood.ANXIOUS, "Reporting a reply", "Tell us when a reply feels hurtful or\nwrong",
        "If a reply feels hurtful or wrong, tap Report under it and pick a reason. Only that reply and the reason you pick are sent to us."),
    HelpTopic(AppIcons.People, Mood.LONELY, "Finding human support", "Doctors, helplines and trained\ncounsellors",
        "If things have felt heavy for a while, talking to a doctor is the best option. In India, Tele-MANAS (14416) is free and open " +
            "24x7, with trained counsellors. For urgent help, call 112. Outside India, your doctor or a local helpline can help.")
)

@Composable
internal fun PrivacyHelpScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var expanded by rememberSaveable { mutableStateOf<Int?>(null) }
    ScrollScreen(AppColors.Background, PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 16.dp)) {
        PageHeader("Privacy & Help", onBack)
        Spacer(Modifier.height(16.dp))
        Column(Modifier.fillMaxWidth().background(AppColors.Mint, RoundedCornerShape(24.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(AppIcons.Phone, contentDescription = null, tint = AppColors.Green, modifier = Modifier.size(24.dp))
                Text("Talk to a person", style = DisplayStyle, fontSize = 18.sp, lineHeight = 24.sp, modifier = Modifier.semantics { heading() })
            }
            Text("Let It Out is an AI and can't call anyone for you. These lines are answered by people.", fontSize = 13.sp, lineHeight = 19.sp)
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HelplineButton("Tele-MANAS 14416", "Mental health · free · 24x7", filled = true, Modifier.weight(1f)) { dialNumber(context, "14416") }
                HelplineButton("Urgent help · 112", "Any time, anywhere in India", filled = false, Modifier.weight(1f)) { dialNumber(context, "112") }
            }
        }
        Spacer(Modifier.height(16.dp))
        val shape = RoundedCornerShape(20.dp)
        Column(Modifier.fillMaxWidth().softShadow(AppColors.Ink, shape, 3.dp).clip(shape).background(AppColors.Surface)) {
            helpTopics.forEachIndexed { index, topic ->
                if (index > 0) RowDivider()
                HelpTopicRow(topic, expanded == index) { expanded = if (expanded == index) null else index }
            }
        }
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(16.dp))
        Text(
            "Let It Out does not diagnose, treat or cure any condition. Helpline numbers are for India and are checked before each release.",
            Modifier.padding(horizontal = 6.dp), color = AppColors.Muted, fontSize = 11.sp, lineHeight = 16.sp
        )
    }
}

@Composable
private fun HelplineButton(title: String, detail: String, filled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick, shape = RoundedCornerShape(16.dp), color = if (filled) AppColors.GreenDeep else AppColors.Surface,
        border = if (filled) null else BorderStroke(1.5.dp, AppColors.Green), modifier = modifier.fillMaxHeight()
    ) {
        Column(Modifier.heightIn(min = 56.dp).padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)) {
            Text(title, color = if (filled) Color.White else AppColors.Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(detail, color = if (filled) Color.White else AppColors.Muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun HelpTopicRow(topic: HelpTopic, expanded: Boolean, onToggle: () -> Unit) {
    val palette = topic.mood.palette()
    Column(
        Modifier.fillMaxWidth().clickable(onClickLabel = if (expanded) "Collapse" else "Expand", onClick = onToggle)
            .semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" }
            .padding(horizontal = 16.dp, vertical = 12.dp).animateContentSize()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(38.dp).background(palette.tile, RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {
                Icon(topic.icon, contentDescription = null, tint = palette.accent, modifier = Modifier.size(18.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(topic.title, fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.Bold)
                Text(topic.summary, color = AppColors.Muted, fontSize = 11.sp, lineHeight = 15.sp)
            }
            Icon(AppIcons.ChevronRight, contentDescription = null, tint = AppColors.Muted, modifier = Modifier.size(18.dp).rotate(if (expanded) 90f else 0f))
        }
        if (expanded) Text(topic.detail, Modifier.padding(start = 48.dp, top = 10.dp), color = AppColors.Muted, fontSize = 14.sp, lineHeight = 21.sp)
    }
}
