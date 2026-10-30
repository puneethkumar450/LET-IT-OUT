package com.letitout.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.letitout.app.data.COMPANION_NAME
import com.letitout.app.data.MoodPalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

/** The chat timer shows calm whole minutes, rounded up so "1 min left" lasts the final minute. */
internal fun minutesLeftLabel(remainingMs: Long): String =
    "${ceil(remainingMs.coerceAtLeast(0L) / 60_000.0).toInt().coerceAtLeast(1)} min left"

/** The live preview shows seconds, as m:ss. */
internal fun liveClockLabel(remainingMs: Long): String {
    val seconds = ceil(remainingMs.coerceAtLeast(0L) / 1000.0).toLong()
    return "%d:%02d".format(seconds / 60, seconds % 60)
}

/** "in about 4 hours", "in about 40 minutes" or "in a few minutes". */
internal fun resetInLabel(resetsAt: Long, now: Long): String {
    val minutes = ((resetsAt - now) / 60_000L).coerceAtLeast(0L)
    return when {
        minutes >= 90 -> "in about ${(minutes + 30) / 60} hours"
        minutes >= 50 -> "in about an hour"
        minutes >= 10 -> "in about ${(minutes + 5) / 10 * 10} minutes"
        else -> "in a few minutes"
    }
}

internal fun shortDateLabel(epochMs: Long, locale: Locale = Locale.getDefault()): String =
    SimpleDateFormat("EEE, d MMM", locale).format(Date(epochMs))

/** Two minutes before a chat ends, the timer and a short note turn warm. */
internal const val CHAT_WARNING_MS = 2 * 60_000L
internal const val LIVE_WARNING_MS = 30_000L

@Composable
internal fun TimerPill(label: String, spokenLabel: String, icon: ImageVector, iconTint: Color, warm: Boolean, compact: Boolean = false) {
    Row(
        Modifier.background(if (warm) AppColors.Peach else AppColors.Surface, CircleShape)
            .heightIn(min = if (compact) 24.dp else 34.dp).padding(horizontal = if (compact) 9.dp else 12.dp)
            .semantics(mergeDescendants = true) { contentDescription = spokenLabel },
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = if (warm) AppColors.Danger else iconTint, modifier = Modifier.size(if (compact) 13.dp else 16.dp))
        Text(label, color = AppColors.Ink, fontSize = if (compact) 12.sp else 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

/** A short note above the composer: the two-minute warning, the live ending, or support mode. */
@Composable
internal fun TrialNote(text: String, icon: ImageVector, warm: Boolean, accent: Color) {
    Row(
        Modifier.fillMaxWidth().background(if (warm) AppColors.Peach.copy(alpha = .6f) else AppColors.Surface, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = if (warm) AppColors.Danger else accent, modifier = Modifier.padding(top = 1.dp).size(18.dp))
        Text(text, color = AppColors.Ink, fontSize = 14.sp, lineHeight = 20.sp)
    }
}

/** Replaces the composer once Ari has said goodbye; the chat stays above to read back. */
@Composable
internal fun TimeUpCard(
    minutes: Int,
    chatsLeft: Int?,
    palette: MoodPalette,
    onLetItGo: () -> Unit,
    onNewChat: () -> Unit,
    onHome: () -> Unit
) {
    val shape = RoundedCornerShape(26.dp)
    Column(
        Modifier.fillMaxWidth().softShadow(palette.accent, shape, 6.dp).background(AppColors.Surface, shape)
            .padding(18.dp).semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("This chat's $minutes minutes are up", style = DisplayStyle, fontSize = 20.sp, modifier = Modifier.semantics { heading() })
        Text(
            "You can read it back above, let it go, or start a fresh chat with $COMPANION_NAME.",
            color = AppColors.Muted, fontSize = 14.sp, lineHeight = 20.sp
        )
        PillButton("Let it go", onLetItGo, color = palette.accent)
        when {
            chatsLeft == null -> OutlinePillButton("Start a new chat", onNewChat)
            chatsLeft > 0 -> OutlinePillButton("Start a new chat · $chatsLeft left today", onNewChat)
            else -> OutlinePillButton("Back to home", onHome)
        }
    }
}

/** Explains the live preview before it starts, so a weekly preview is never used by accident. */
@Composable
internal fun BoxScope.LiveIntroSheet(seconds: Int, palette: MoodPalette, onStart: () -> Unit, onDismiss: () -> Unit) {
    Box(
        Modifier.matchParentSize().background(AppColors.Ink.copy(alpha = .38f))
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
    )
    Column(
        Modifier.align(Alignment.BottomCenter).fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)).background(AppColors.Surface)
            .clickable(remember { MutableInteractionSource() }, indication = null) { }
            .navigationBarsPadding().padding(start = 20.dp, top = 14.dp, end = 20.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(Modifier.align(Alignment.CenterHorizontally).size(40.dp, 5.dp).background(AppColors.Track, CircleShape))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(48.dp).background(palette.bubble, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                Icon(AppIcons.Video, contentDescription = null, tint = palette.accent, modifier = Modifier.size(24.dp))
            }
            Text("Talk to $COMPANION_NAME face to face", style = DisplayStyle, fontSize = 21.sp, modifier = Modifier.semantics { heading() })
        }
        val minutes = (seconds + 59) / 60
        IntroLine(AppIcons.Clock, palette.accent, "$minutes minute${if (minutes == 1) "" else "s"}, once a week while we're in early access. It only counts, and the timer only starts, once $COMPANION_NAME appears.")
        IntroLine(AppIcons.Chat, palette.accent, "When it ends, your chat carries on in text. Nothing is lost.")
        IntroLine(AppIcons.Shield, palette.accent, "$COMPANION_NAME's spoken replies go to our video partner to move her face. Your camera is never used.")
        PillButton("Start live preview", onStart, color = palette.accent)
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            LinkButton("Not now, keep typing", palette.accent, onDismiss)
        }
    }
}

@Composable
private fun IntroLine(icon: ImageVector, accent: Color, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.padding(top = 2.dp).size(20.dp))
        Text(text, color = AppColors.Ink, fontSize = 15.sp, lineHeight = 22.sp)
    }
}

/**
 * Shown over chat when it can't go on: today's free chats are used ([busy] false), or the shared
 * free quota is ([busy] true). Helplines stay one tap away in both.
 */
@Composable
internal fun TrialBlockScreen(
    busy: Boolean,
    resetsAt: Long?,
    now: Long,
    palette: MoodPalette,
    onBack: () -> Unit,
    onTryAgain: () -> Unit,
    onDial: (String) -> Unit
) {
    Column(
        Modifier.fillMaxSize().background(palette.surface).statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        SquareIconButton(AppIcons.Back, "Back to home", onBack, shadowColor = palette.accent)
        Column(
            Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(Modifier.size(132.dp).background(palette.bubble, CircleShape).padding(8.dp)) {
                CompanionPortrait(Modifier.fillMaxSize().clip(CircleShape), contentDescription = null)
            }
            Text(
                if (busy) "$COMPANION_NAME is taking a short break" else "You've had today's free chats",
                style = DisplayStyle, fontSize = 25.sp, lineHeight = 31.sp, textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                if (busy) "A lot of people are talking to $COMPANION_NAME right now, and we've reached our free limit for now. " +
                    "Please try again in about an hour. This doesn't use up any of your free chats."
                else "Thank you for making time for yourself. $COMPANION_NAME will be ready again at midnight" +
                    (resetsAt?.let { ", ${resetInLabel(it, now)}." } ?: "."),
                color = AppColors.Muted, fontSize = 16.sp, lineHeight = 23.sp, textAlign = TextAlign.Center
            )
            if (busy) OutlinePillButton("Try again", onTryAgain)
        }
        Spacer(Modifier.weight(1f, fill = false))
        HelplineCard(onDial)
    }
}

/** Tele-MANAS and 112, for every screen where Ari can't talk right now. */
@Composable
internal fun HelplineCard(onDial: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().background(AppColors.Surface, RoundedCornerShape(24.dp)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Need to talk to someone now?", color = AppColors.Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text("Tele-MANAS is free, confidential and open all day and night.", color = AppColors.Muted, fontSize = 14.sp, lineHeight = 20.sp)
        PillButton("Call Tele-MANAS · 14416", { onDial("14416") })
        OutlinePillButton("Call emergency · 112", { onDial("112") })
    }
}
