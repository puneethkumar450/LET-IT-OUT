package com.letitout.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.letitout.app.R
import com.letitout.app.data.Mood

object AppColors {
    val Background = Color(0xFFF8F7FF)
    val Surface = Color(0xFFFFFFFF)
    val Ink = Color(0xFF26243D)
    val Muted = Color(0xFF5E5B7A)
    val Primary = Color(0xFF6A5AE0)
    val BrandTint = Color(0xFF8B7CF6)
    val Lavender = Color(0xFFE6E1FF)
    val Track = Color(0xFFD6D2F0)
    val Divider = Color(0xFFF0EEF9)
    val Peach = Color(0xFFFFE3D6)
    val Mint = Color(0xFFCFF2E6)
    val Green = Color(0xFF238D7C)
    val GreenDeep = Color(0xFF1F7F70)   // filled buttons: white text on it passes AA contrast
    val Sky = Color(0xFF70B8FF)
    val Error = Color(0xFFC9633F)
    val Warning = Color(0xFFE0A33D)
}

private val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

// Downloaded through Google Play services; the system font is used until (or if) they arrive.
val Fredoka = FontFamily(
    Font(GoogleFont("Fredoka"), fontProvider, FontWeight.Medium),
    Font(GoogleFont("Fredoka"), fontProvider, FontWeight.SemiBold)
)
val Nunito = FontFamily(
    Font(GoogleFont("Nunito"), fontProvider, FontWeight.Normal),
    Font(GoogleFont("Nunito"), fontProvider, FontWeight.SemiBold),
    Font(GoogleFont("Nunito"), fontProvider, FontWeight.Bold)
)

val DisplayStyle = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, color = AppColors.Ink)

val AppTypography: Typography = Typography().let { base ->
    Typography(
        displayLarge = base.displayLarge.copy(fontFamily = Fredoka),
        displayMedium = base.displayMedium.copy(fontFamily = Fredoka),
        displaySmall = base.displaySmall.copy(fontFamily = Fredoka),
        headlineLarge = base.headlineLarge.copy(fontFamily = Fredoka),
        headlineMedium = base.headlineMedium.copy(fontFamily = Fredoka),
        headlineSmall = base.headlineSmall.copy(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontFamily = Nunito, fontWeight = FontWeight.Bold),
        titleMedium = base.titleMedium.copy(fontFamily = Nunito, fontWeight = FontWeight.Bold),
        titleSmall = base.titleSmall.copy(fontFamily = Nunito, fontWeight = FontWeight.Bold),
        bodyLarge = base.bodyLarge.copy(fontFamily = Nunito, letterSpacing = 0.sp),
        bodyMedium = base.bodyMedium.copy(fontFamily = Nunito, letterSpacing = 0.sp),
        bodySmall = base.bodySmall.copy(fontFamily = Nunito, letterSpacing = 0.sp),
        labelLarge = base.labelLarge.copy(fontFamily = Nunito, fontWeight = FontWeight.Bold, letterSpacing = 0.sp),
        labelMedium = base.labelMedium.copy(fontFamily = Nunito, letterSpacing = 0.sp),
        labelSmall = base.labelSmall.copy(fontFamily = Nunito, letterSpacing = 0.sp)
    )
}

/** Soft, tinted elevation used for cards and buttons. */
fun Modifier.softShadow(color: Color, shape: Shape, elevation: Dp = 6.dp) =
    shadow(elevation, shape, clip = false, ambientColor = color, spotColor = color)

private fun circle(cx: Float, cy: Float, r: Float) =
    "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0z"

private fun rect(x: Float, y: Float, w: Float, h: Float, r: Float) =
    "M${x + r} ${y}h${w - 2 * r}a$r $r 0 0 1 $r ${r}v${h - 2 * r}a$r $r 0 0 1 ${-r} ${r}" +
        "h${-(w - 2 * r)}a$r $r 0 0 1 ${-r} ${-r}v${-(h - 2 * r)}a$r $r 0 0 1 $r ${-r}z"

private fun buildIcon(paths: Array<out String>, filled: Boolean, width: Float): ImageVector =
    ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
        paths.forEach { data ->
            addPath(
                pathData = addPathNodes(data),
                fill = if (filled) SolidColor(Color.Black) else null,
                stroke = if (filled) null else SolidColor(Color.Black),
                strokeLineWidth = width,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }
    }.build()

private fun lineIcon(vararg paths: String, width: Float = 2f) = buildIcon(paths, filled = false, width)
private fun solidIcon(vararg paths: String) = buildIcon(paths, filled = true, 0f)

/** Rounded line icons traced from the design's 24px SVGs. */
object AppIcons {
    val Back by lazy { lineIcon("M15 5l-7 7 7 7") }
    val ChevronRight by lazy { lineIcon("M9 5l7 7-7 7") }
    val Settings by lazy {
        lineIcon(
            circle(12f, 12f, 3f),
            "M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z"
        )
    }
    val Flame by lazy { lineIcon("M12 3s5 4 5 9.5a5 5 0 0 1-10 0c0-2 1-3.5 2-4.5.5 1.5 1.5 2.5 2 2.5.5-2 0-4.5 1-7.5z") }
    val Drop by lazy { lineIcon("M12 3s6 6.5 6 11a6 6 0 0 1-12 0c0-4.5 6-11 6-11z") }
    val Waves by lazy { lineIcon("M3 9l3-3 3 3 3-3 3 3 3-3 3 3", "M3 16l3-3 3 3 3-3 3 3 3-3 3 3") }
    val Spiral by lazy { lineIcon("M12 12a2 2 0 0 1 2 2 3 3 0 0 1-5 2.2 5 5 0 0 1 3-8.2 7 7 0 0 1 7 7") }
    val Moon by lazy { lineIcon("M20 14.5A8 8 0 1 1 9.5 4a6.5 6.5 0 0 0 10.5 10.5z") }
    val Sun by lazy {
        lineIcon(circle(12f, 12f, 4f), "M12 2v2M12 20v2M2 12h2M20 12h2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4")
    }
    val Chat by lazy { lineIcon("M4 6a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H9l-5 4V6z") }
    val Heart by lazy { solidIcon("M12 21c-7-4.5-10-8.5-10-13a5 5 0 0 1 9-3 5 5 0 0 1 9 3c0 4.5-3 8.5-8 13z") }
    val More by lazy { solidIcon(circle(5f, 12f, 2f), circle(12f, 12f, 2f), circle(19f, 12f, 2f)) }
    val Mic by lazy { lineIcon(rect(9f, 3f, 6f, 11f, 3f), "M5 11a7 7 0 0 0 14 0M12 18v3") }
    val Send by lazy { lineIcon("M12 19V5M6 11l6-6 6 6", width = 2.2f) }
    val Stop by lazy { solidIcon(rect(5f, 5f, 14f, 14f, 3f)) }
    val Shield by lazy { lineIcon("M12 3l8 3v6c0 5-3.5 8.5-8 9-4.5-.5-8-4-8-9V6l8-3z") }
    val ShieldCheck by lazy { lineIcon("M12 3l8 3v6c0 5-3.5 8.5-8 9-4.5-.5-8-4-8-9V6l8-3z", "M9 12l2 2 4-4") }
    val Phone by lazy { lineIcon("M5 4h4l2 5-2.5 1.5a11 11 0 0 0 5 5L15 13l5 2v4a2 2 0 0 1-2 2A16 16 0 0 1 3 6a2 2 0 0 1 2-2z") }
    val Retry by lazy {
        lineIcon("M5 12.5a7 7 0 0 1 11.6-5.2L19 9.5M19 5v4.5h-4.5", "M19 11.5a7 7 0 0 1-11.6 5.2L5 14.5M5 19v-4.5h4.5")
    }
    val Sync by lazy { lineIcon("M4 12a8 8 0 0 1 14-5.3L20 9M20 4v5h-5", "M20 12a8 8 0 0 1-14 5.3L4 15M4 20v-5h5") }
    val Storage by lazy { lineIcon(rect(4f, 5f, 16f, 14f, 3f), "M4 11h16") }
    val Trash by lazy { lineIcon("M4 7h16M10 11v6M14 11v6M6 7l1 13h10l1-13M9 7V4h6v3") }
    val People by lazy {
        lineIcon(circle(9f, 8f, 3.5f), "M2.5 20a6.5 6.5 0 0 1 13 0M16 4a3.5 3.5 0 0 1 0 7M21.5 20a6.5 6.5 0 0 0-4.5-6.2")
    }
    val Mail by lazy { lineIcon(rect(3f, 5f, 18f, 14f, 3f), "M3 8l9 6 9-6") }
    val Flag by lazy { lineIcon("M5 21V4", "M5 4h12l-2.5 4.5L17 13H5") }
}

val Mood.icon: ImageVector
    get() = when (this) {
        Mood.ANGRY -> AppIcons.Flame
        Mood.SAD -> AppIcons.Drop
        Mood.STRESSED -> AppIcons.Waves
        Mood.ANXIOUS -> AppIcons.Spiral
        Mood.LONELY -> AppIcons.Moon
        Mood.HAPPY -> AppIcons.Sun
        Mood.TALK -> AppIcons.Chat
    }

// The brand mark: a speech bubble holding a heart (72px artwork).
private val BrandGlyph by lazy {
    ImageVector.Builder(defaultWidth = 72.dp, defaultHeight = 72.dp, viewportWidth = 72f, viewportHeight = 72f)
        .addPath(
            addPathNodes("M12 14h48a8 8 0 0 1 8 8v28a8 8 0 0 1-8 8H30l-14 10v-10h-4a8 8 0 0 1-8-8V22a8 8 0 0 1 8-8z"),
            fill = SolidColor(AppColors.BrandTint)
        )
        .addPath(
            addPathNodes("M36 50c-8-5.5-15-10.5-15-17.4 0-4.4 3.4-7.6 7.4-7.6 2.9 0 5.5 1.6 7.6 4.3 2.1-2.7 4.7-4.3 7.6-4.3 4 0 7.4 3.2 7.4 7.6C51 39.5 44 44.5 36 50z"),
            fill = SolidColor(Color.White)
        )
        .build()
}

@Composable
fun BrandMark(modifier: Modifier = Modifier, size: Dp = 64.dp, corner: Dp = 22.dp, glyph: Dp = 36.dp) {
    val shape = RoundedCornerShape(corner)
    Box(
        modifier.size(size).softShadow(AppColors.Primary, shape, size * 0.12f).background(AppColors.Surface, shape),
        contentAlignment = Alignment.Center
    ) {
        Image(rememberVectorPainter(BrandGlyph), contentDescription = null, modifier = Modifier.size(glyph))
    }
}
