package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.example.camera.model.GridModeOption
import com.example.ui.theme.LumoraFocusYellow
import com.example.ui.theme.LumoraSuccessGreen
import com.example.ui.theme.LumoraWhite20

@Composable
fun GridOverlay(
    gridMode: GridModeOption,
    levelerEnabled: Boolean,
    rollAngle: Float,
    isLevel: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // 1. Grid Lines
        if (gridMode == GridModeOption.RULE_OF_THIRDS) {
            val gridColor = Color.White.copy(alpha = 0.22f)
            val stroke = 1.dp.toPx()

            // Vertical lines at 1/3 and 2/3
            val x1 = width / 3f
            val x2 = width * 2f / 3f
            drawLine(gridColor, Offset(x1, 0f), Offset(x1, height), stroke)
            drawLine(gridColor, Offset(x2, 0f), Offset(x2, height), stroke)

            // Horizontal lines at 1/3 and 2/3
            val y1 = height / 3f
            val y2 = height * 2f / 3f
            drawLine(gridColor, Offset(0f, y1), Offset(width, y1), stroke)
            drawLine(gridColor, Offset(0f, y2), Offset(width, y2), stroke)
        } else if (gridMode == GridModeOption.GOLDEN_RATIO) {
            val gridColor = Color.White.copy(alpha = 0.20f)
            val stroke = 1.dp.toPx()
            val phi = 0.618f
            val x1 = width * (1f - phi)
            val x2 = width * phi
            val y1 = height * (1f - phi)
            val y2 = height * phi

            drawLine(gridColor, Offset(x1, 0f), Offset(x1, height), stroke)
            drawLine(gridColor, Offset(x2, 0f), Offset(x2, height), stroke)
            drawLine(gridColor, Offset(0f, y1), Offset(width, y1), stroke)
            drawLine(gridColor, Offset(0f, y2), Offset(width, y2), stroke)
        }

        // 2. Horizon Leveler
        if (levelerEnabled) {
            val centerX = width / 2f
            val centerY = height / 2f
            val lineLength = 54.dp.toPx()
            val gap = 12.dp.toPx()

            // Fixed horizontal reticle reference dots
            val dotColor = Color.White.copy(alpha = 0.35f)
            drawCircle(dotColor, radius = 2.dp.toPx(), center = Offset(centerX - lineLength - 8.dp.toPx(), centerY))
            drawCircle(dotColor, radius = 2.dp.toPx(), center = Offset(centerX + lineLength + 8.dp.toPx(), centerY))

            // Rotating tilt bar
            rotate(degrees = -rollAngle, pivot = Offset(centerX, centerY)) {
                val barColor = if (isLevel) LumoraFocusYellow else Color.White.copy(alpha = 0.75f)
                val barThickness = if (isLevel) 2.2.dp.toPx() else 1.4.dp.toPx()

                // Left wing
                drawLine(
                    color = barColor,
                    start = Offset(centerX - lineLength, centerY),
                    end = Offset(centerX - gap, centerY),
                    strokeWidth = barThickness,
                    cap = StrokeCap.Round
                )
                // Right wing
                drawLine(
                    color = barColor,
                    start = Offset(centerX + gap, centerY),
                    end = Offset(centerX + lineLength, centerY),
                    strokeWidth = barThickness,
                    cap = StrokeCap.Round
                )

                // Center cross indicator when perfectly level
                if (isLevel) {
                    drawCircle(
                        color = LumoraFocusYellow,
                        radius = 2.5.dp.toPx(),
                        center = Offset(centerX, centerY)
                    )
                }
            }
        }
    }
}
