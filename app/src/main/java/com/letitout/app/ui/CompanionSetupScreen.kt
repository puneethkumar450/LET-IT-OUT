package com.letitout.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.letitout.app.R

/** Introduces Ari with full-screen artwork before first sign-in. */
@Composable
internal fun CompanionSetupScreen(onContinue: () -> Unit, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val pulse by rememberInfiniteTransition(label = "listening glow").animateFloat(
        initialValue = .45f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1250), RepeatMode.Reverse),
        label = "glow alpha"
    )

    Box(Modifier.fillMaxSize().background(Color(0xFF362844))) {
        Image(
            painter = painterResource(R.drawable.ari_onboarding_fullscreen),
            contentDescription = "Ari, your AI companion",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color(0x660F0D26),
                    .24f to Color.Transparent,
                    .54f to Color.Transparent,
                    .75f to Color(0xA82B213E),
                    1f to Color(0xFF241A36)
                )
            )
        )
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                .padding(start = 24.dp, top = 12.dp, end = 24.dp, bottom = 20.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(onClick = onBack, shape = CircleShape, color = Color.Black.copy(alpha = .28f)) {
                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        Icon(AppIcons.Back, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(Modifier.weight(1f))
                Text("LET IT GO", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
            }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Box(Modifier.size(9.dp).alpha(pulse).background(Color(0xFFBCEFE0), CircleShape))
                Text("A SPACE TO BE HEARD", color = Color.White.copy(alpha = .9f), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Meet Ari.", style = DisplayStyle, color = Color.White, fontSize = 44.sp,
                lineHeight = 49.sp, modifier = Modifier.semantics { heading() }
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Speak or type what's on your mind. Ari replies aloud with a face that moves with the voice.",
                color = Color.White.copy(alpha = .94f), fontSize = 17.sp, lineHeight = 24.sp
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Spoken replies go to Simli for lip sync. Your chat summary is one tap away.",
                color = Color.White.copy(alpha = .75f), fontSize = 14.sp, lineHeight = 20.sp
            )
            Spacer(Modifier.height(24.dp))
            Surface(onClick = onContinue, shape = CircleShape, color = Color.White, modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.height(58.dp), contentAlignment = Alignment.Center) {
                    Text("Continue with Ari", color = Color(0xFF332542), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
