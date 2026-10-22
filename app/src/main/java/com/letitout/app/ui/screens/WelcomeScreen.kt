package com.letitout.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.letitout.app.data.Mood
import com.letitout.app.ui.AppColors
import com.letitout.app.ui.BrandMark
import com.letitout.app.ui.MoodSymbol

@Composable
internal fun WelcomeScreen(onStart: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 26.dp, vertical = 24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BrandMark(size = 42.dp)
            Spacer(Modifier.width(12.dp))
            Text("LET IT OUT", color = AppColors.Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 2.sp)
        }
        Spacer(Modifier.height(54.dp))
        Text("A little room\nto be yourself.", color = AppColors.Ink, fontSize = 42.sp, lineHeight = 47.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(14.dp))
        Text("Whatever today feels like, you can start here.", color = AppColors.Muted, fontSize = 17.sp, lineHeight = 24.sp)
        Spacer(Modifier.height(34.dp))
        Surface(
            color = AppColors.PrimarySoft,
            shape = RoundedCornerShape(30.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(24.dp)) {
                Box(Modifier.size(68.dp).background(AppColors.Card, RoundedCornerShape(22.dp)), contentAlignment = Alignment.Center) {
                    MoodSymbol(Mood.TALK, Modifier.size(38.dp), AppColors.Primary)
                }
                Spacer(Modifier.height(28.dp))
                Text("No perfect words needed.", color = AppColors.Ink, fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text("Say what's on your mind, however it comes out. Or simply look around first.", color = AppColors.Muted, fontSize = 14.sp, lineHeight = 21.sp)
            }
        }
        Spacer(Modifier.height(30.dp))
        listOf(
            "A place to check in with yourself",
            "Talk or type, at your own pace",
            "Look around first, sign in when ready"
        ).forEach { line ->
            Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).background(AppColors.Primary, CircleShape))
                Spacer(Modifier.width(13.dp))
                Text(line, color = AppColors.Ink, fontSize = 14.sp)
            }
        }
        Spacer(Modifier.height(32.dp))
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(19.dp)) {
            Text("Get started", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
        }
        Spacer(Modifier.height(15.dp))
        Text("For adults 18+ · Not a replacement for a doctor or professional care", modifier = Modifier.fillMaxWidth(), color = AppColors.Muted, fontSize = 11.sp, lineHeight = 16.sp, textAlign = TextAlign.Center)
    }
}
