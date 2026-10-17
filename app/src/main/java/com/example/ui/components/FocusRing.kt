package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LumoraFocusYellow
import kotlin.math.roundToInt

/**
 * Apple-inspired Focus and Exposure Indicator.
 * Renders an animated yellow square with an exposure adjustment sun slider.
 */
@Composable
fun FocusRing(
    x: Float,
    y: Float,
    exposureOffset: Float, // -2f to +2f
    onExposureChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val scale = remember { Animatable(1.35f) }
    val alpha = remember { Animatable(1f) }

    LaunchedEffect(x, y) {
        scale.snapTo(1.35f)
        alpha.snapTo(1f)
        scale.animateTo(1.0f, tween(260, easing = FastOutSlowInEasing))
    }

    val boxSize = 72.dp
    val boxSizePx = 72f * 2.5f // Approximate dp to px for center offset

    Box(
        modifier = modifier
            .offset { IntOffset((x - boxSizePx / 2f).roundToInt(), (y - boxSizePx / 2f).roundToInt()) }
            .size(boxSize)
            .pointerInput(x, y) {
                detectVerticalDragGestures { _, dragAmount ->
                    val delta = -dragAmount / 120f
                    val newEv = (exposureOffset + delta).coerceIn(-2.0f, 2.0f)
                    onExposureChanged(newEv)
                }
            }
    ) {
        Canvas(modifier = Modifier.size(boxSize)) {
            val strokeWidth = 1.6.dp.toPx()
            val cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            val ringColor = LumoraFocusYellow.copy(alpha = alpha.value)

            // Outer rounded square
            val currentScale = scale.value
            val rectWidth = size.width * currentScale
            val rectHeight = size.height * currentScale
            val left = (size.width - rectWidth) / 2f
            val top = (size.height - rectHeight) / 2f

            drawRoundRect(
                color = ringColor,
                topLeft = Offset(left, top),
                size = Size(rectWidth, rectHeight),
                cornerRadius = cornerRadius,
                style = Stroke(width = strokeWidth)
            )

            // Little corner ticks / crosshairs on four edges
            val midX = size.width / 2f
            val midY = size.height / 2f
            val tickLen = 5.dp.toPx()

            drawLine(ringColor, Offset(midX, top - tickLen), Offset(midX, top), strokeWidth)
            drawLine(ringColor, Offset(midX, top + rectHeight), Offset(midX, top + rectHeight + tickLen), strokeWidth)
            drawLine(ringColor, Offset(left - tickLen, midY), Offset(left, midY), strokeWidth)
            drawLine(ringColor, Offset(left + rectWidth, midY), Offset(left + rectWidth + tickLen, midY), strokeWidth)

            // Exposure Sun Indicator on the right edge
            val sunX = left + rectWidth + 14.dp.toPx()
            val sunY = midY - (exposureOffset * 16.dp.toPx())
            drawCircle(ringColor, radius = 4.dp.toPx(), center = Offset(sunX, sunY))

            // Exposure vertical slider line
            drawLine(
                color = ringColor.copy(alpha = 0.4f),
                start = Offset(sunX, midY - 32.dp.toPx()),
                end = Offset(sunX, midY + 32.dp.toPx()),
                strokeWidth = 1.dp.toPx()
            )
        }
    }
}
