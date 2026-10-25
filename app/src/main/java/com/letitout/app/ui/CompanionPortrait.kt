package com.letitout.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.letitout.app.R
import kotlin.math.roundToInt

/**
 * Displays one cell from the selected companion's 3 × 3 expression sheet.
 * The sheets use identical framing in each cell, so changing expression keeps
 * the face at the same size and position. Only the selected sheet is decoded.
 *
 * The caller controls the frame. For a round portrait, apply `clip(CircleShape)`
 * to [modifier]. The image itself remains square so it can also fill a hero.
 */
@Composable
fun CompanionPortrait(
    gender: String,
    age: String,
    expression: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val resourceId = companionSheet(gender, age)
    val image = ImageBitmap.imageResource(id = resourceId)
    val cell = expressionCell(expression)
    val describedModifier = if (contentDescription.isNullOrBlank()) modifier else {
        modifier.semantics { this.contentDescription = contentDescription }
    }

    Canvas(describedModifier.aspectRatio(1f)) {
        val cellWidth = image.width / 3
        val cellHeight = image.height / 3
        drawImage(
            image = image,
            srcOffset = IntOffset(cell.first * cellWidth, cell.second * cellHeight),
            srcSize = IntSize(cellWidth, cellHeight),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
            filterQuality = FilterQuality.High
        )
    }
}

private fun companionSheet(gender: String, age: String): Int = when (gender.lowercase() to age.lowercase()) {
    "woman" to "young" -> R.drawable.companion_woman_young
    "woman" to "mature" -> R.drawable.companion_woman_mature
    "man" to "young" -> R.drawable.companion_man_young
    "man" to "adult" -> R.drawable.companion_man_adult
    "man" to "mature" -> R.drawable.companion_man_mature
    else -> R.drawable.companion_woman_adult
}

private fun expressionCell(expression: String): Pair<Int, Int> = when (expression.lowercase()) {
    "angry" -> 1 to 0
    "sad" -> 2 to 0
    "stressed" -> 0 to 1
    "anxious" -> 1 to 1
    "lonely" -> 2 to 1
    "happy" -> 0 to 2
    "worried" -> 1 to 2
    "doubtful" -> 2 to 2
    else -> 0 to 0
}
