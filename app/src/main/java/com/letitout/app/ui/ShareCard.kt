package com.letitout.app.ui

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import com.letitout.app.data.MoodPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Fixed share text. The card and text never include message text, mood, intensity or Ari's replies. */
internal const val RELEASE_SHARE_TEXT = "I let go of something today. let-it-go.in"
private const val SHARE_DIR = "share"
private const val CARD_WIDTH = 1080
private const val CARD_HEIGHT = 1350

/** Draws the release card from a fixed template: gradient, a frozen particle frame and fixed text. */
internal fun drawReleaseCard(palette: MoodPalette): Bitmap {
    val bitmap = Bitmap.createBitmap(CARD_WIDTH, CARD_HEIGHT, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val w = CARD_WIDTH.toFloat()
    val h = CARD_HEIGHT.toFloat()
    canvas.drawRect(0f, 0f, w, h, Paint().apply {
        shader = LinearGradient(0f, 0f, 0f, h, palette.tile.toArgb(), palette.surface.toArgb(), Shader.TileMode.CLAMP)
    })
    val dot = Paint(Paint.ANTI_ALIAS_FLAG)
    val tones = palette.particleTones()
    // Particles are sized in dp on screen; 3px per dp keeps them in proportion on a 1080px card.
    releaseParticles(count = 90, seed = 11).forEach { particle ->
        // Leave the text band clear so the words stay easy to read.
        particle.frameAt(.5f)?.takeUnless { it.y in .24f..0.46f }?.let { frame ->
            dot.color = tones[particle.tone].copy(alpha = frame.alpha * .85f).toArgb()
            canvas.drawCircle(frame.x * w, frame.y * h, frame.radiusDp * 3f, dot)
        }
    }
    val ink = AppColors.Ink.toArgb()
    val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ink; textSize = 84f; textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    canvas.drawText("I let go of", w / 2, h * .36f, title)
    canvas.drawText("something today.", w / 2, h * .36f + 104f, title)
    val brand = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = palette.accent.toArgb(); textSize = 46f; textAlign = Paint.Align.CENTER; letterSpacing = .18f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    canvas.drawText("LET IT GO", w / 2, h - 170f, brand)
    val site = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AppColors.Muted.toArgb(); textSize = 36f; textAlign = Paint.Align.CENTER
    }
    canvas.drawText("let-it-go.in", w / 2, h - 110f, site)
    return bitmap
}

/** Writes the card to the app cache, replacing any earlier one, and opens the share sheet. */
internal suspend fun shareReleaseCard(context: Context, palette: MoodPalette) {
    val file = withContext(Dispatchers.IO) {
        deleteReleaseCards(context)
        val dir = File(context.cacheDir, SHARE_DIR).apply { mkdirs() }
        val bitmap = drawReleaseCard(palette)
        File(dir, "let-it-go.png").also { out ->
            out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, RELEASE_SHARE_TEXT)
        clipData = ClipData.newRawUri(null, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, "Share").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
}

/** Removes shared cards from the cache; called before each new card and when the end screen closes. */
internal fun deleteReleaseCards(context: Context) {
    File(context.cacheDir, SHARE_DIR).listFiles()?.forEach { it.delete() }
}
