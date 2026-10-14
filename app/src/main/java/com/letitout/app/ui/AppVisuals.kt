package com.letitout.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.letitout.app.data.Mood
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

object AppColors {
    val Canvas = Color(0xFFF7F6F2)
    val Card = Color(0xFFFFFEFB)
    val Ink = Color(0xFF203A35)
    val Muted = Color(0xFF71817A)
    val Primary = Color(0xFF276759)
    val PrimarySoft = Color(0xFFE7F0EA)
    val Line = Color(0xFFDCE5DE)
    val Warm = Color(0xFFF5ECE5)
    val Error = Color(0xFF944E42)
}

@Composable
fun BrandMark(modifier: Modifier = Modifier, size: Dp = 64.dp) {
    Box(
        modifier.size(size).background(AppColors.Primary, RoundedCornerShape(size * 0.31f)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(size * 0.57f)) {
            val w = this.size.width
            val h = this.size.height
            val heart = Path().apply {
                moveTo(w * .5f, h * .84f)
                cubicTo(w * .38f, h * .73f, w * .12f, h * .54f, w * .12f, h * .33f)
                cubicTo(w * .12f, h * .12f, w * .37f, h * .09f, w * .5f, h * .29f)
                cubicTo(w * .63f, h * .09f, w * .88f, h * .12f, w * .88f, h * .33f)
                cubicTo(w * .88f, h * .54f, w * .62f, h * .73f, w * .5f, h * .84f)
            }
            drawPath(
                heart, Color.White,
                style = Stroke(width = min(w, h) * .075f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

@Composable
fun MoodSymbol(mood: Mood, modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = min(w, h) * .07f
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
            drawLine(color, Offset(w * x1, h * y1), Offset(w * x2, h * y2), strokeWidth, StrokeCap.Round)

        when (mood) {
            Mood.ANGRY -> {
                val bolt = Path().apply {
                    moveTo(w * .58f, h * .08f)
                    lineTo(w * .26f, h * .54f)
                    lineTo(w * .49f, h * .54f)
                    lineTo(w * .38f, h * .92f)
                    lineTo(w * .77f, h * .40f)
                    lineTo(w * .53f, h * .40f)
                    close()
                }
                drawPath(bolt, color.copy(alpha = .14f))
                drawPath(bolt, color, style = stroke)
            }
            Mood.SAD -> {
                val drop = Path().apply {
                    moveTo(w * .5f, h * .09f)
                    cubicTo(w * .39f, h * .28f, w * .2f, h * .48f, w * .2f, h * .64f)
                    cubicTo(w * .2f, h * .83f, w * .34f, h * .92f, w * .5f, h * .92f)
                    cubicTo(w * .66f, h * .92f, w * .8f, h * .83f, w * .8f, h * .64f)
                    cubicTo(w * .8f, h * .48f, w * .61f, h * .28f, w * .5f, h * .09f)
                    close()
                }
                drawPath(drop, color.copy(alpha = .13f))
                drawPath(drop, color, style = stroke)
            }
            Mood.STRESSED -> {
                for (i in 0..2) {
                    val y = .25f + i * .25f
                    val wave = Path().apply {
                        moveTo(w * .12f, h * y)
                        cubicTo(w * .30f, h * (y - .17f), w * .42f, h * (y + .17f), w * .59f, h * y)
                        cubicTo(w * .70f, h * (y - .11f), w * .79f, h * (y - .10f), w * .89f, h * y)
                    }
                    drawPath(wave, color, style = stroke)
                }
            }
            Mood.ANXIOUS -> {
                drawArc(
                    color, startAngle = 35f, sweepAngle = 290f, useCenter = false,
                    topLeft = Offset(w * .12f, h * .12f), size = Size(w * .76f, h * .76f), style = stroke
                )
                drawArc(
                    color, startAngle = 205f, sweepAngle = 225f, useCenter = false,
                    topLeft = Offset(w * .32f, h * .32f), size = Size(w * .36f, h * .36f), style = stroke
                )
                drawCircle(color, radius = strokeWidth * .7f, center = Offset(w * .51f, h * .49f))
            }
            Mood.LONELY -> {
                val moon = Path().apply {
                    moveTo(w * .62f, h * .11f)
                    cubicTo(w * .34f, h * .17f, w * .2f, h * .37f, w * .25f, h * .60f)
                    cubicTo(w * .31f, h * .86f, w * .59f, h * .91f, w * .82f, h * .72f)
                    cubicTo(w * .51f, h * .76f, w * .33f, h * .39f, w * .62f, h * .11f)
                    close()
                }
                drawPath(moon, color.copy(alpha = .14f))
                drawPath(moon, color, style = stroke)
                drawCircle(color, radius = strokeWidth * .62f, center = Offset(w * .78f, h * .2f))
            }
            Mood.HAPPY -> {
                drawCircle(color, radius = w * .21f, center = Offset(w * .5f, h * .5f), style = stroke)
                for (i in 0 until 8) {
                    val angle = Math.PI * 2 * i / 8
                    val dx = cos(angle).toFloat()
                    val dy = sin(angle).toFloat()
                    line(.5f + dx * .34f, .5f + dy * .34f, .5f + dx * .45f, .5f + dy * .45f)
                }
            }
            Mood.TALK -> {
                val bubble = Path().apply {
                    moveTo(w * .22f, h * .16f)
                    lineTo(w * .78f, h * .16f)
                    quadraticTo(w * .89f, h * .16f, w * .89f, h * .29f)
                    lineTo(w * .89f, h * .64f)
                    quadraticTo(w * .89f, h * .77f, w * .76f, h * .77f)
                    lineTo(w * .42f, h * .77f)
                    lineTo(w * .23f, h * .91f)
                    lineTo(w * .26f, h * .77f)
                    quadraticTo(w * .11f, h * .77f, w * .11f, h * .64f)
                    lineTo(w * .11f, h * .29f)
                    quadraticTo(w * .11f, h * .16f, w * .22f, h * .16f)
                    close()
                }
                drawPath(bubble, color, style = stroke)
                listOf(.36f, .5f, .64f).forEach { x ->
                    drawCircle(color, radius = strokeWidth * .56f, center = Offset(w * x, h * .47f))
                }
            }
        }
    }
}
