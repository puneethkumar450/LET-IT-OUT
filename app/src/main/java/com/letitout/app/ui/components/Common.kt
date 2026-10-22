package com.letitout.app.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.letitout.app.ui.AppColors

internal const val TELE_MANAS_NUMBER = "14416"

/** Opens the phone dialer with Tele-MANAS filled in. Nothing is dialled until the person taps call. */
internal fun dialTeleManas(context: Context) {
    try {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$TELE_MANAS_NUMBER")))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "You can call $TELE_MANAS_NUMBER from any phone. It's free.", Toast.LENGTH_LONG).show()
    }
}

@Composable
internal fun PageBackButton(onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(44.dp).background(AppColors.PrimarySoft, CircleShape)) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AppColors.Primary, modifier = Modifier.size(22.dp))
    }
}

@Composable
internal fun SettingsItem(icon: ImageVector, title: String, detail: String, destructive: Boolean = false, onClick: () -> Unit) {
    val tint = if (destructive) AppColors.Error else AppColors.Primary
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = AppColors.Card), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, AppColors.Line)) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).background(if (destructive) AppColors.Warm else AppColors.PrimarySoft, RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(15.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = if (destructive) AppColors.Error else AppColors.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(detail, color = AppColors.Muted, fontSize = 12.sp, lineHeight = 17.sp)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = AppColors.Muted)
        }
    }
}

@Composable
internal fun InformationCard(
    icon: ImageVector,
    title: String,
    body: String,
    warm: Boolean = false,
    action: (@Composable () -> Unit)? = null
) {
    Surface(
        color = if (warm) AppColors.Warm else AppColors.Card,
        shape = RoundedCornerShape(22.dp),
        border = if (warm) null else BorderStroke(1.dp, AppColors.Line)
    ) {
        Column(Modifier.fillMaxWidth().padding(19.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).background(if (warm) AppColors.Card else AppColors.PrimarySoft, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = AppColors.Primary, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(13.dp))
                Text(title, color = AppColors.Ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(13.dp))
            Text(body, color = if (warm) AppColors.Ink else AppColors.Muted, fontSize = 14.sp, lineHeight = 21.sp)
            if (action != null) {
                Spacer(Modifier.height(15.dp))
                action()
            }
        }
    }
}
