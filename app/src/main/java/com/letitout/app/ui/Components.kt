package com.letitout.app.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri

/** A full-screen page that scrolls when needed; `Spacer(Modifier.weight(1f))` pushes content to the bottom. */
@Composable
fun ScrollScreen(
    background: Color,
    padding: PaddingValues,
    decor: @Composable BoxScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Box(Modifier.fillMaxSize().background(background)) {
        decor()
        BoxWithConstraints(Modifier.fillMaxSize().systemBarsPadding()) {
            val viewportHeight = maxHeight
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Column(Modifier.fillMaxWidth().heightIn(min = viewportHeight).padding(padding), content = content)
            }
        }
    }
}

@Composable
fun Blob(size: Dp, color: Color, modifier: Modifier = Modifier) {
    Box(modifier.size(size).background(color, CircleShape))
}

@Composable
fun PillButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = AppColors.Primary, enabled: Boolean = true) {
    Button(
        onClick = onClick, enabled = enabled, shape = CircleShape, elevation = null,
        colors = ButtonDefaults.buttonColors(
            containerColor = color, contentColor = Color.White,
            disabledContainerColor = color.copy(alpha = .4f), disabledContentColor = Color.White
        ),
        modifier = modifier.fillMaxWidth().height(56.dp).then(if (enabled) Modifier.softShadow(color, CircleShape, 8.dp) else Modifier)
    ) { Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
}

@Composable
fun OutlinePillButton(text: String, onClick: () -> Unit, enabled: Boolean = true, leading: @Composable () -> Unit = {}) {
    OutlinedButton(
        onClick = onClick, enabled = enabled, shape = CircleShape,
        border = BorderStroke(1.5.dp, AppColors.Track),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = AppColors.Surface, contentColor = AppColors.Ink,
            disabledContainerColor = AppColors.Surface, disabledContentColor = AppColors.Muted
        ),
        modifier = Modifier.fillMaxWidth().height(56.dp).softShadow(AppColors.Ink, CircleShape, 3.dp)
    ) {
        leading()
        Spacer(Modifier.width(12.dp))
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun LinkButton(text: String, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = onClick, modifier = modifier.heightIn(min = 44.dp), colors = ButtonDefaults.textButtonColors(contentColor = color)) {
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SquareIconButton(icon: ImageVector, description: String, onClick: () -> Unit, shadowColor: Color = AppColors.Ink, size: Dp = 48.dp) {
    val shape = RoundedCornerShape(16.dp)
    Surface(onClick = onClick, shape = shape, color = AppColors.Surface, modifier = Modifier.size(size).softShadow(shadowColor, shape, 4.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = description, tint = AppColors.Ink, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
fun ChipButton(text: String, onClick: () -> Unit, container: Color, content: Color, icon: ImageVector? = null, iconTint: Color = content) {
    Surface(onClick = onClick, shape = CircleShape, color = container, contentColor = content, modifier = Modifier.heightIn(min = 44.dp)) {
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            icon?.let { Icon(it, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp)) }
            Text(text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SmallPillButton(text: String, onClick: () -> Unit, container: Color, content: Color) {
    Surface(onClick = onClick, shape = CircleShape, color = container, contentColor = content) {
        Box(Modifier.heightIn(min = 40.dp).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
            Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PageHeader(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SquareIconButton(AppIcons.Back, "Back", onBack)
        Text(title, style = DisplayStyle, fontSize = 26.sp, modifier = Modifier.semantics { heading() })
    }
}

@Composable
fun SettingsGroup(label: String, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            label, Modifier.padding(horizontal = 6.dp).semantics { heading() },
            color = AppColors.Muted, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = .4.sp
        )
        Column(Modifier.fillMaxWidth().softShadow(AppColors.Ink, shape, 3.dp).clip(shape).background(AppColors.Surface), content = content)
    }
}

@Composable
fun RowDivider() = HorizontalDivider(color = AppColors.Divider, thickness = 1.dp)

@Composable
fun SettingsRow(
    title: String, onClick: () -> Unit, value: String? = null, chevron: Boolean = true,
    color: Color = AppColors.Ink, bold: Boolean = false, detail: String? = null
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = color, fontSize = 16.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
            detail?.let { Text(it, color = AppColors.Muted, fontSize = 13.sp, lineHeight = 18.sp) }
        }
        value?.let { Text(it, color = AppColors.Primary, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
        if (chevron) Icon(AppIcons.ChevronRight, contentDescription = null, tint = AppColors.Muted, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun ToggleRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, detail: String? = null) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontSize = 16.sp)
            detail?.let { Text(it, color = AppColors.Muted, fontSize = 13.sp, lineHeight = 18.sp) }
        }
        Switch(
            checked = checked, onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White, checkedTrackColor = AppColors.Primary, checkedBorderColor = AppColors.Primary,
                uncheckedThumbColor = Color.White, uncheckedTrackColor = AppColors.Track, uncheckedBorderColor = AppColors.Track
            )
        )
    }
}

/** Opens the dialer with [number] filled in; the person still chooses to place the call. */
fun dialNumber(context: Context, number: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_DIAL, "tel:$number".toUri()))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "Call $number from your phone.", Toast.LENGTH_LONG).show()
    }
}
