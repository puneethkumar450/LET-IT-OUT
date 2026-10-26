package com.letitout.app.ui

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.letitout.app.data.TEST_BUILD_NOTICE

/** The single post-setup starting point. The live connection opens only after Start talking. */
@Composable
internal fun HomeScreen(
    hasSummary: Boolean,
    onStartTalking: () -> Unit,
    onViewSummary: () -> Unit,
    onSettings: () -> Unit
) {
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
                    .27f to Color.Transparent,
                    .56f to Color.Transparent,
                    .77f to Color(0xB52B213E),
                    1f to Color(0xFF241A36)
                )
            )
        )
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                .padding(start = 24.dp, top = 12.dp, end = 24.dp, bottom = 20.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "LET IT GO", color = Color.White, fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp
                )
                Spacer(Modifier.weight(1f))
                Surface(onClick = onSettings, shape = CircleShape, color = Color.Black.copy(alpha = .28f)) {
                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        Icon(AppIcons.Settings, contentDescription = "Settings", tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Box(Modifier.size(9.dp).background(Color(0xFFBCEFE0), CircleShape))
                Text(
                    "ARI · READY WHEN YOU ARE", color = Color.White.copy(alpha = .9f),
                    fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "A place to let it out.", style = DisplayStyle, color = Color.White,
                fontSize = 40.sp, lineHeight = 45.sp, modifier = Modifier.semantics { heading() }
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Talk to Ari about whatever is on your mind. Speak or type, and go at your own pace.",
                color = Color.White.copy(alpha = .92f), fontSize = 17.sp, lineHeight = 24.sp
            )
            Spacer(Modifier.height(24.dp))
            Surface(onClick = onStartTalking, shape = CircleShape, color = Color.White, modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.height(58.dp), contentAlignment = Alignment.Center) {
                    Text("Start talking", color = Color(0xFF332542), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (hasSummary) {
                Spacer(Modifier.height(8.dp))
                Surface(onClick = onViewSummary, shape = CircleShape, color = Color.White.copy(alpha = .16f), modifier = Modifier.fillMaxWidth()) {
                    Box(Modifier.height(50.dp), contentAlignment = Alignment.Center) {
                        Text("Read chat summary", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "$TEST_BUILD_NOTICE For adults 18+.",
                color = Color.White.copy(alpha = .8f), fontSize = 12.sp, lineHeight = 17.sp
            )
        }
    }
}
