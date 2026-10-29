package com.letitout.app.ui

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.letitout.app.data.ChatMessage
import com.letitout.app.data.Mood
import com.letitout.app.data.MoodPalette
import com.letitout.app.data.palette
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** True when the app setting asks for it or the system has animations turned off. */
@Composable
internal fun rememberReduceMotion(setting: Boolean): Boolean {
    val context = LocalContext.current
    val systemOff = remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    return setting || systemOff
}

/** One drifting speck of the dissolve. Positions and distances are fractions of the canvas. */
internal data class ReleaseParticle(
    val x: Float, val y: Float, val drift: Float, val rise: Float,
    val radiusDp: Float, val delay: Float, val tone: Int
)

/** Where a particle is at [progress], or null while it is hidden. Alpha rises then falls. */
internal data class ParticleFrame(val x: Float, val y: Float, val radiusDp: Float, val alpha: Float)

internal fun releaseParticles(count: Int = 72, seed: Int = 7): List<ReleaseParticle> {
    val random = Random(seed)
    return List(count) {
        val y = .35f + random.nextFloat() * .55f
        ReleaseParticle(
            x = .08f + random.nextFloat() * .84f,
            y = y,
            drift = (random.nextFloat() - .5f) * .16f,
            // A share of the start height, so a particle never rises past the top edge.
            rise = y * (.4f + random.nextFloat() * .5f),
            radiusDp = 2f + random.nextFloat() * 5f,
            delay = random.nextFloat() * .3f,
            tone = random.nextInt(3)
        )
    }
}

internal fun ReleaseParticle.frameAt(progress: Float): ParticleFrame? {
    val local = (progress - .15f - delay) / .55f
    if (local <= 0f || local >= 1f) return null
    return ParticleFrame(
        x = x + drift * local,
        y = y - rise * local,
        radiusDp = radiusDp * (1f - .5f * local),
        alpha = sin(PI * local).toFloat()
    )
}

internal fun MoodPalette.particleTones() = listOf(accent, bubble, Color.White)

/**
 * The last few bubbles drift up and fade while particles rise, then [onFinished] runs once.
 * With reduced motion the bubbles only fade.
 */
@Composable
internal fun ReleaseAnimation(
    messages: List<ChatMessage>,
    palette: MoodPalette,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
    onFinished: () -> Unit
) {
    val progress = remember { Animatable(0f) }
    val haptics = LocalHapticFeedback.current
    val finished by rememberUpdatedState(onFinished)
    val particles = remember { releaseParticles() }
    val tones = remember(palette) { palette.particleTones() }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(if (reduceMotion) 600 else 2600, easing = LinearEasing))
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        finished()
    }
    Box(
        modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(palette.tile, palette.surface)))
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = "Letting go of this conversation"
            }
    ) {
        val recent = messages.takeLast(5)
        Column(
            Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 16.dp, vertical = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Bottom)
        ) {
            recent.forEachIndexed { index, message ->
                val start = if (reduceMotion) 0f else index * .06f
                val span = if (reduceMotion) 1f else .55f
                Row(
                    Modifier.fillMaxWidth().graphicsLayer {
                        val local = FastOutSlowInEasing.transform(((progress.value - start) / span).coerceIn(0f, 1f))
                        alpha = 1f - local
                        if (!reduceMotion) {
                            translationY = -local * 160.dp.toPx()
                            scaleX = 1f - .08f * local
                            scaleY = 1f - .08f * local
                        }
                    },
                    horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start
                ) {
                    Text(
                        message.text,
                        Modifier.widthIn(max = 280.dp)
                            .background(if (message.fromUser) palette.bubble else AppColors.Surface, RoundedCornerShape(22.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        color = AppColors.Ink, fontSize = 16.sp, lineHeight = 23.sp, maxLines = 4, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        if (!reduceMotion) Canvas(Modifier.fillMaxSize()) {
            particles.forEach { particle ->
                particle.frameAt(progress.value)?.let { frame ->
                    drawCircle(
                        tones[particle.tone].copy(alpha = frame.alpha * .9f),
                        radius = frame.radiusDp.dp.toPx(),
                        center = Offset(frame.x * size.width, frame.y * size.height)
                    )
                }
            }
        }
    }
}

/**
 * Shown after a release. A conversation that had a crisis-support reply leads with helplines and
 * offers no share card.
 */
@Composable
internal fun ReleaseRitualScreen(hadSupport: Boolean, onTalkAgain: () -> Unit, onHome: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val palette = Mood.TALK.palette()
    var shareError by remember { mutableStateOf<String?>(null) }
    DisposableEffect(context) { onDispose { deleteReleaseCards(context) } }
    ScrollScreen(
        palette.surface, PaddingValues(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 16.dp),
        decor = {
            Blob(220.dp, palette.tile, Modifier.offset(x = (-70).dp, y = (-40).dp))
            Blob(160.dp, palette.bubble.copy(alpha = .6f), Modifier.align(Alignment.BottomEnd).offset(x = 50.dp, y = (-140).dp))
        }
    ) {
        Spacer(Modifier.weight(1f))
        if (hadSupport) {
            Text("It's cleared from the app.", style = DisplayStyle, fontSize = 30.sp, lineHeight = 36.sp, modifier = Modifier.semantics { heading() })
            Spacer(Modifier.height(12.dp))
            Text(
                "If things still feel heavy, please talk to a person now. These lines are answered by people.",
                color = AppColors.Ink, fontSize = 17.sp, lineHeight = 25.sp
            )
            Spacer(Modifier.height(24.dp))
            PillButton("Call Tele-MANAS · 14416", { dialNumber(context, "14416") })
            Spacer(Modifier.height(12.dp))
            OutlinePillButton("Call emergency · 112", { dialNumber(context, "112") })
        } else {
            Text("Gone.", style = DisplayStyle, fontSize = 40.sp, modifier = Modifier.semantics { heading() })
            Spacer(Modifier.height(8.dp))
            Text("You don't have to carry that.", color = AppColors.Ink, fontSize = 20.sp, lineHeight = 28.sp)
            Spacer(Modifier.height(10.dp))
            Text("This conversation is cleared from the app.", color = AppColors.Muted, fontSize = 14.sp, lineHeight = 20.sp)
        }
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(24.dp))
        PillButton(if (hadSupport) "Talk to Ari again" else "Talk again", onTalkAgain, color = if (hadSupport) AppColors.Ink else palette.accent)
        if (!hadSupport) {
            Spacer(Modifier.height(12.dp))
            OutlinePillButton("Share", {
                shareError = null
                scope.launch {
                    try { shareReleaseCard(context, palette) }
                    catch (_: Exception) { shareError = "Sharing isn't available right now." }
                }
            }) { Icon(AppIcons.Share, contentDescription = null, tint = palette.accent, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.height(6.dp))
            Text(
                "The image only says you let something go. None of your words are in it.",
                Modifier.fillMaxWidth(), color = AppColors.Muted, fontSize = 12.sp, lineHeight = 17.sp, textAlign = TextAlign.Center
            )
            shareError?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, Modifier.fillMaxWidth(), color = AppColors.Danger, fontSize = 13.sp, textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.height(8.dp))
        LinkButton("Home", AppColors.Primary, onHome, Modifier.align(Alignment.CenterHorizontally))
    }
}
