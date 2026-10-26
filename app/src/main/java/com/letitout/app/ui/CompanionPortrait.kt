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

/** A face crop of the same generated Ari portrait used on the full-screen setup. */
@Composable
fun CompanionPortrait(modifier: Modifier = Modifier, contentDescription: String? = null) {
    val image = ImageBitmap.imageResource(id = R.drawable.ari_onboarding_fullscreen)
    val describedModifier = if (contentDescription.isNullOrBlank()) modifier else {
        modifier.semantics { this.contentDescription = contentDescription }
    }

    Canvas(describedModifier.aspectRatio(1f)) {
        // Keep Ari's face prominent at small sizes without creating a second portrait asset.
        val cropSize = (image.width * .68f).roundToInt()
        val cropLeft = (image.width - cropSize) / 2
        val cropTop = (image.height * .055f).roundToInt()
        drawImage(
            image = image,
            srcOffset = IntOffset(cropLeft, cropTop),
            srcSize = IntSize(cropSize, cropSize),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
            filterQuality = FilterQuality.High
        )
    }
}
