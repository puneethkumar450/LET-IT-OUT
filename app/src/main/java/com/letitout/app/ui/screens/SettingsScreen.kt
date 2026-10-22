package com.letitout.app.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.letitout.app.reminder.ReminderNotifications
import com.letitout.app.ui.AppColors
import com.letitout.app.ui.AppViewModel
import com.letitout.app.ui.BrandMark
import com.letitout.app.ui.components.PageBackButton
import com.letitout.app.ui.components.SettingsItem

private val reminderTimes = listOf("9 AM" to 9, "2 PM" to 14, "8 PM" to 20, "10 PM" to 22)
private const val NOTIFICATIONS_OFF =
    "Notifications are turned off for LET IT OUT, so the check-in can't show up yet. You can turn them on in your phone settings."
private const val NOTIFICATIONS_DECLINED =
    "That's okay. If you'd like a daily check-in later, you can allow notifications for LET IT OUT in your phone settings."

@Composable
internal fun SettingsScreen(vm: AppViewModel, onBack: () -> Unit, onPrivacy: () -> Unit, onAuth: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var status by remember { mutableStateOf("") }
    var note by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    // Re-checked on every return to the screen, so turning notifications off (or back on) in phone settings shows up here.
    var canPost by remember { mutableStateOf(ReminderNotifications.canPost(context)) }
    LifecycleResumeEffect(Unit) {
        canPost = ReminderNotifications.canPost(context)
        if (canPost && (note == NOTIFICATIONS_OFF || note == NOTIFICATIONS_DECLINED)) note = null
        onPauseOrDispose { }
    }
    val signedIn = vm.auth.isSignedIn()
    val guest = signedIn && vm.auth.isGuest()
    val enableReminder: () -> Unit = {
        vm.setReminder(true, state.reminder.hour, state.reminder.minute)
        canPost = ReminderNotifications.canPost(context)
        note = null // the "notifications are off" line below follows canPost on its own
    }
    val shownNote = note ?: if (state.reminder.enabled && !canPost) NOTIFICATIONS_OFF else null
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
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 24.dp)) {
        PageBackButton(onBack)
        Spacer(Modifier.height(34.dp))
        Text("YOUR SPACE", color = AppColors.Primary, fontSize = 11.sp, letterSpacing = 1.9.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(9.dp))
        Text("Settings", fontSize = 36.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Ink)
        Spacer(Modifier.height(9.dp))
        Text("Keep your space comfortable and in your control.", color = AppColors.Muted, fontSize = 15.sp, lineHeight = 22.sp)
        Spacer(Modifier.height(30.dp))
        Surface(color = AppColors.Card, shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, AppColors.Line)) {
            Row(Modifier.fillMaxWidth().padding(19.dp), verticalAlignment = Alignment.CenterVertically) {
                BrandMark(size = 52.dp)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (guest) "Guest" else if (signedIn) vm.auth.userName() ?: "Your account" else "Exploring in preview", color = AppColors.Ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(3.dp))
                    Text(if (guest) "Signed in as a guest" else if (signedIn) "Signed in" else "No account connected", color = AppColors.Muted, fontSize = 13.sp)
                }
            }
        }
        Spacer(Modifier.height(33.dp))
        Text("DAILY CHECK-IN", color = AppColors.Primary, fontSize = 11.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(13.dp))
        Surface(color = AppColors.Card, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, AppColors.Line)) {
            Column(Modifier.fillMaxWidth().padding(15.dp)) {
                // The whole row is the switch, so TalkBack reads "Daily check-in" with its on/off state.
                Row(
                    Modifier.fillMaxWidth().toggleable(value = state.reminder.enabled, role = Role.Switch, onValueChange = onReminderToggle),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(46.dp).background(AppColors.PrimarySoft, RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = AppColors.Primary, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(15.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Daily check-in", color = AppColors.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("A gentle note once a day, in case you'd like to talk.", color = AppColors.Muted, fontSize = 12.sp, lineHeight = 17.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Switch(
                        checked = state.reminder.enabled,
                        onCheckedChange = null,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = AppColors.Primary,
                            uncheckedTrackColor = AppColors.PrimarySoft,
                            uncheckedThumbColor = AppColors.Muted,
                            uncheckedBorderColor = AppColors.Line
                        )
                    )
                }
                Spacer(Modifier.height(15.dp))
                Text("Remind me at", color = AppColors.Muted, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    reminderTimes.forEach { (label, hour) ->
                        val isSelected = state.reminder.hour == hour && state.reminder.minute == 0
                        Surface(
                            // Picking a time while the check-in is on reschedules it; while off it just remembers the choice.
                            onClick = { vm.setReminder(state.reminder.enabled, hour, 0) },
                            color = if (isSelected) AppColors.Primary else AppColors.PrimarySoft,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).semantics { selected = isSelected; role = Role.RadioButton }
                        ) {
                            Text(
                                label,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                                textAlign = TextAlign.Center,
                                color = if (isSelected) Color.White else AppColors.Primary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
        shownNote?.let { Spacer(Modifier.height(12.dp)); Text(it, color = AppColors.Muted, fontSize = 13.sp, lineHeight = 19.sp) }
        Spacer(Modifier.height(11.dp))
        SettingsItem(Icons.Default.Refresh, "Clear check-in history", "Your \"How are you feeling now?\" answers on this phone", onClick = {
            vm.clearCheckIns()
            note = "Check-in history cleared."
        })
        Spacer(Modifier.height(33.dp))
        Text("ACCOUNT & PRIVACY", color = AppColors.Primary, fontSize = 11.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(13.dp))
        SettingsItem(Icons.Default.Lock, "Privacy and support", "How this space works", onClick = onPrivacy)
        Spacer(Modifier.height(11.dp))
        if (signedIn) {
            if (guest) {
                // Upgrading keeps everything on this phone, unlike signing out.
                SettingsItem(Icons.Default.AccountCircle, "Keep your space with Google", "Switch from guest to your Google account", onClick = onAuth)
                Spacer(Modifier.height(11.dp))
            }
            SettingsItem(Icons.AutoMirrored.Filled.ExitToApp, "Sign out", "Clears check-ins and the reminder on this phone", onClick = {
                status = ""
                // Check-ins and the reminder aren't tied to an account, so they go too, ready for whoever signs in next.
                vm.signOut(onDone = onAuth)
            })
            Spacer(Modifier.height(11.dp))
            SettingsItem(Icons.Default.Delete, "Delete my account", "Remove your sign-in account", destructive = true, onClick = { confirmDelete = true })
        } else {
            SettingsItem(Icons.Default.AccountCircle, "Sign in or join as guest", "Sign in whenever you're ready to talk", onClick = onAuth)
            Spacer(Modifier.height(20.dp))
            Surface(color = AppColors.PrimarySoft, shape = RoundedCornerShape(20.dp)) {
                Text("Preview mode lets you explore the app. There is no account to sign out of or delete.", modifier = Modifier.fillMaxWidth().padding(17.dp), color = AppColors.Muted, fontSize = 13.sp, lineHeight = 19.sp)
            }
        }
        if (status.isNotBlank()) { Spacer(Modifier.height(15.dp)); Text(status, color = AppColors.Error, fontSize = 13.sp, lineHeight = 19.sp) }
        Spacer(Modifier.height(41.dp))
        Text("LET IT OUT  ·  VERSION 0.1.0 PREVIEW", color = AppColors.Muted, fontSize = 11.sp, letterSpacing = .6.sp)
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Delete your account?") },
        text = { Text("This removes your sign-in account and the check-ins saved on this phone, and turns off the daily check-in. Conversations aren't stored on our server in this preview.") },
        confirmButton = { TextButton(onClick = {
            confirmDelete = false
            status = ""
            vm.deleteAccount(onDone = onAuth, onError = { status = it })
        }) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
    )
}
