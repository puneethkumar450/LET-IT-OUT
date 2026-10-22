package com.letitout.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.letitout.app.ui.AppColors
import com.letitout.app.ui.components.InformationCard
import com.letitout.app.ui.components.PageBackButton
import com.letitout.app.ui.components.TELE_MANAS_NUMBER
import com.letitout.app.ui.components.dialTeleManas

@Composable
internal fun PrivacyScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 24.dp)) {
        PageBackButton(onBack)
        Spacer(Modifier.height(34.dp))
        Text("GOOD TO KNOW", color = AppColors.Primary, fontSize = 11.sp, letterSpacing = 1.9.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(9.dp))
        Text("Privacy & support", fontSize = 34.sp, lineHeight = 39.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Ink)
        Spacer(Modifier.height(10.dp))
        Text("Clear information for a more comfortable conversation.", color = AppColors.Muted, fontSize = 15.sp, lineHeight = 22.sp)
        Spacer(Modifier.height(30.dp))
        InformationCard(
            Icons.Default.Info, "What this app is",
            "LET IT OUT is an AI listening companion, not a therapist or a doctor. It's here to listen, and it can sometimes get things wrong."
        )
        Spacer(Modifier.height(12.dp))
        InformationCard(
            Icons.Default.Lock, "When you use the chat",
            "Your messages are sent to our server and AI provider to create a reply. Please don't share anything you aren't comfortable sending to those services."
        )
        Spacer(Modifier.height(12.dp))
        InformationCard(
            Icons.Default.CheckCircle, "Your check-ins",
            "Your \"How are you feeling now?\" answers and your reminder time stay on this phone. You can clear them anytime in Settings."
        )
        Spacer(Modifier.height(12.dp))
        InformationCard(
            Icons.Default.Phone, "Support is always here",
            "If things have felt heavy for a while, talking to a doctor is the best option. In India, Tele-MANAS ($TELE_MANAS_NUMBER) is free and open 24x7, " +
                "with trained counsellors. For urgent help, call 112. Outside India, your doctor or a local helpline can help.",
            warm = true,
            action = {
                Button(onClick = { dialTeleManas(context) }, shape = RoundedCornerShape(15.dp)) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("Call $TELE_MANAS_NUMBER", fontWeight = FontWeight.SemiBold)
                }
            }
        )
        Spacer(Modifier.height(25.dp))
        Text("ABOUT THIS PREVIEW", color = AppColors.Primary, fontSize = 11.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text("We do not currently save chat history on our server. This is a prototype; final privacy, terms and deletion policies must be published before release.", color = AppColors.Muted, fontSize = 13.sp, lineHeight = 20.sp)
        Spacer(Modifier.height(24.dp))
    }
}
