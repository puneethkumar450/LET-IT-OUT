package com.letitout.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.letitout.app.data.Mood
import com.letitout.app.data.MoodPalette
import com.letitout.app.data.palette
import kotlin.math.roundToInt

@Composable
internal fun HomeScreen(
    onMood: (Mood) -> Unit,
    onJustTalk: () -> Unit,
    onLetsTalk: () -> Unit,
    onNewConversation: () -> Unit,
    onSettings: () -> Unit
) {
    ScrollScreen(AppColors.Background, PaddingValues(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Hello there", color = AppColors.Muted, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Icon(AppIcons.Heart, contentDescription = null, tint = AppColors.Sky, modifier = Modifier.size(20.dp))
                }
                Text("How are you feeling today?", style = DisplayStyle, fontSize = 30.sp, lineHeight = 35.sp, modifier = Modifier.semantics { heading() })
            }
            SquareIconButton(AppIcons.Settings, "Settings", onSettings, size = 42.dp)
        }
        Spacer(Modifier.height(22.dp))
        Mood.entries.filter { it != Mood.TALK }.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { mood -> MoodTile(mood, Modifier.weight(1f)) { onMood(mood) } }
            }
        }
        Surface(onClick = onJustTalk, shape = RoundedCornerShape(22.dp), color = AppColors.Lavender, contentColor = AppColors.Ink, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.height(74.dp).padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                MoodBadge(Mood.TALK, AppColors.Primary)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Just want to talk", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("No label needed — start anywhere", color = AppColors.Muted, fontSize = 14.sp)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(22.dp))
        PillButton("Let's Talk", onLetsTalk)
        Spacer(Modifier.height(8.dp))
        LinkButton("Start a new conversation", AppColors.Primary, onNewConversation, Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun MoodTile(mood: Mood, modifier: Modifier, onClick: () -> Unit) {
    val palette = mood.palette()
    Surface(onClick = onClick, shape = RoundedCornerShape(22.dp), color = palette.tile, contentColor = AppColors.Ink, modifier = modifier) {
        Box(Modifier.height(132.dp)) {
            MoodBadge(mood, palette.accent, Modifier.align(Alignment.TopStart).padding(16.dp))
            MoodSticker(
                mood = mood,
                palette = palette,
                size = 66.dp,
                emojiSize = 39.sp,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 12.dp, end = 14.dp)
            )
            Text(mood.label, Modifier.align(Alignment.BottomStart).padding(16.dp), fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MoodBadge(mood: Mood, tint: Color, modifier: Modifier = Modifier) {
    Box(modifier.size(40.dp).background(AppColors.Surface, CircleShape), contentAlignment = Alignment.Center) {
        Icon(mood.icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

private val Mood.stickerEmoji: String
    get() = when (this) {
        Mood.ANGRY -> "😠"
        Mood.SAD -> "😢"
        Mood.STRESSED -> "😫"
        Mood.ANXIOUS -> "😰"
        Mood.LONELY -> "🥺"
        Mood.HAPPY -> "😄"
        Mood.TALK -> "💬"
    }

@Composable
private fun MoodSticker(mood: Mood, palette: MoodPalette, size: Dp, emojiSize: TextUnit, modifier: Modifier = Modifier) {
    val rotation = when (mood) {
        Mood.ANGRY, Mood.STRESSED, Mood.LONELY -> -8f
        Mood.SAD, Mood.ANXIOUS, Mood.HAPPY -> 8f
        Mood.TALK -> 0f
    }
    Box(
        modifier.rotate(rotation).size(size)
            .softShadow(palette.accent.copy(alpha = 0.3f), CircleShape, 5.dp)
            .background(AppColors.Surface, CircleShape)
            .border(1.dp, palette.accent.copy(alpha = 0.14f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(mood.stickerEmoji, Modifier.clearAndSetSemantics { }, fontSize = emojiSize, lineHeight = emojiSize)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MoodCheckInScreen(
    mood: Mood,
    palette: MoodPalette,
    onBack: () -> Unit,
    onChangeMood: () -> Unit,
    onStart: (intensity: Int?) -> Unit
) {
    var intensity by rememberSaveable(mood) { mutableIntStateOf(if (mood == Mood.ANGRY) 7 else 5) }
    ScrollScreen(palette.surface, PaddingValues(start = 24.dp, top = 12.dp, end = 24.dp, bottom = 16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            SquareIconButton(AppIcons.Back, "Back", onBack, shadowColor = palette.accent)
            ChipButton("Change mood", onChangeMood, container = AppColors.Surface, content = palette.accent)
        }
        Spacer(Modifier.height(24.dp))
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(palette.bubble)) {
            Blob(180.dp, Color.White.copy(alpha = .55f), Modifier.align(Alignment.TopEnd).offset(60.dp, (-60).dp))
            Column(
                Modifier.fillMaxWidth().padding(start = 24.dp, top = 32.dp, end = 24.dp, bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                MoodSticker(mood, palette, size = 88.dp, emojiSize = 53.sp)
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(mood.heading, style = DisplayStyle, fontSize = 30.sp, lineHeight = 35.sp, textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
                    Text(mood.reassurance, Modifier.widthIn(max = 280.dp), color = AppColors.Muted, fontSize = 16.sp, lineHeight = 24.sp, textAlign = TextAlign.Center)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        val cardShape = RoundedCornerShape(24.dp)
        Column(
            Modifier.fillMaxWidth().softShadow(palette.accent, cardShape, 3.dp).background(AppColors.Surface, cardShape).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("How intense is this feeling?", Modifier.weight(1f), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("Optional", color = AppColors.Muted, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 3.dp))
            }
            Slider(
                value = intensity.toFloat(), onValueChange = { intensity = it.roundToInt() },
                valueRange = 1f..10f, steps = 8,
                thumb = { Box(Modifier.size(18.dp).background(palette.accent, CircleShape)) },
                track = { state ->
                    Box(Modifier.fillMaxWidth().height(5.dp).background(palette.bubble, CircleShape)) {
                        Box(Modifier.fillMaxWidth((state.value - 1f) / 9f).fillMaxHeight().background(palette.accent, CircleShape))
                    }
                },
                modifier = Modifier.height(32.dp).semantics { contentDescription = "Feeling intensity" }
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("1 · A little", color = AppColors.Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("$intensity", color = palette.accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("10 · A lot", color = AppColors.Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Text(
                "This just helps the conversation start in the right place. It isn't a measurement of anything.",
                color = AppColors.Muted, fontSize = 13.sp, lineHeight = 19.sp
            )
        }
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(24.dp))
        PillButton("Start talking", { onStart(intensity) }, color = palette.accent)
        Spacer(Modifier.height(8.dp))
        LinkButton("Skip the rating", palette.accent, { onStart(null) }, Modifier.align(Alignment.CenterHorizontally))
    }
}
