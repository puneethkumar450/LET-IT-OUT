package com.letitout.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.letitout.app.data.CheckInStats
import com.letitout.app.data.Mood
import com.letitout.app.data.palette
import com.letitout.app.ui.AppColors
import com.letitout.app.ui.AppViewModel
import com.letitout.app.ui.BrandMark
import com.letitout.app.ui.MoodSymbol
import java.time.LocalTime

@Composable
internal fun HomeScreen(vm: AppViewModel, onChoose: (Mood) -> Unit, onSettings: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val greeting = if (state.signedIn) greetingFor(LocalTime.now().hour, firstName(state.userName)) else null
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 24.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            BrandMark(size = 44.dp)
            Spacer(Modifier.width(11.dp))
            Text("LET IT OUT", modifier = Modifier.weight(1f), color = AppColors.Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
            IconButton(onClick = onSettings, modifier = Modifier.background(AppColors.Card, CircleShape)) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = AppColors.Primary)
            }
        }
        Spacer(Modifier.height(39.dp))
        if (greeting != null) {
            Text(greeting, color = AppColors.Primary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
        }
        Text("How are you\nfeeling today?", fontSize = 36.sp, lineHeight = 41.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Ink)
        Spacer(Modifier.height(11.dp))
        Text("Choose what feels closest. You can change your mind anytime.", color = AppColors.Muted, fontSize = 15.sp, lineHeight = 22.sp)
        if (state.checkInStats.total > 0) {
            Spacer(Modifier.height(22.dp))
            InsightCard(state.checkInStats)
        }
        Spacer(Modifier.height(34.dp))
        Text("CHECK IN", color = AppColors.Primary, fontSize = 11.sp, letterSpacing = 1.9.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        Mood.entries.dropLast(1).chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { mood ->
                    MoodCard(mood, modifier = Modifier.weight(1f).height(157.dp), onClick = { onChoose(mood) })
                }
            }
        }
        MoodCard(Mood.TALK, modifier = Modifier.fillMaxWidth().height(104.dp), wide = true, onClick = { onChoose(Mood.TALK) })
        Spacer(Modifier.height(24.dp))
        Surface(color = AppColors.PrimarySoft, shape = RoundedCornerShape(24.dp)) {
            Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                MoodSymbol(Mood.TALK, Modifier.size(29.dp), AppColors.Primary)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Your pace. Your space.", color = AppColors.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("No need to have it all figured out.", color = AppColors.Muted, fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

private fun firstName(name: String?): String? = name?.trim()?.substringBefore(' ')?.takeIf { it.isNotBlank() }

private fun greetingFor(hour: Int, name: String?): String {
    val who = name?.let { ", $it" }.orEmpty()
    return when (hour) {
        in 5..11 -> "Good morning$who"
        in 12..16 -> "Good afternoon$who"
        in 17..21 -> "Good evening$who"
        else -> "Up late$who? Glad you're here."
    }
}

private fun insightText(stats: CheckInStats): String = when {
    stats.lighter == 0 -> "Some talks take a while to settle, and that's okay. This space is here whenever you need it."
    stats.total == 1 -> "You felt lighter after your last talk."
    else -> "You felt lighter after ${stats.lighter} of your last ${stats.total} talks."
}

@Composable
private fun InsightCard(stats: CheckInStats) {
    val sun = Mood.HAPPY.palette()
    Surface(color = AppColors.Card, shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, AppColors.Line)) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).background(sun.background, RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) {
                MoodSymbol(Mood.HAPPY, Modifier.size(26.dp), sun.primary)
            }
            Spacer(Modifier.width(15.dp))
            Text(insightText(stats), color = AppColors.Ink, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun MoodCard(mood: Mood, modifier: Modifier, wide: Boolean = false, onClick: () -> Unit) {
    val palette = mood.palette()
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = AppColors.Card),
        shape = RoundedCornerShape(23.dp),
        border = BorderStroke(1.dp, AppColors.Line)
    ) {
        if (wide) {
            Row(Modifier.fillMaxSize().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(60.dp).background(palette.background, RoundedCornerShape(19.dp)), contentAlignment = Alignment.Center) {
                    MoodSymbol(mood, Modifier.size(31.dp), palette.primary)
                }
                Spacer(Modifier.width(17.dp))
                Column(Modifier.weight(1f)) {
                    Text(mood.label, color = AppColors.Ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text(mood.detail, color = AppColors.Muted, fontSize = 12.sp)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = AppColors.Muted)
            }
        } else {
            Column(Modifier.fillMaxSize().padding(17.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Box(Modifier.size(57.dp).background(palette.background, RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                    MoodSymbol(mood, Modifier.size(32.dp), palette.primary)
                }
                Column {
                    Text(mood.label, color = AppColors.Ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text(mood.detail, color = AppColors.Muted, fontSize = 11.sp, maxLines = 1)
                }
            }
        }
    }
}
