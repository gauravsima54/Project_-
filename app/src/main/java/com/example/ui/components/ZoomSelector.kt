package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LumoraFocusYellow
import kotlin.math.abs

@Composable
fun ZoomSelector(
    currentZoom: Float,
    minZoom: Float = 1.0f,
    maxZoom: Float = 8.0f,
    onZoomSelected: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    // Determine available stops based on hardware limits
    val stops = mutableListOf<Float>()
    if (minZoom <= 0.6f) {
        stops.add(0.5f)
    }
    stops.add(1.0f)
    if (maxZoom >= 2.0f) {
        stops.add(2.0f)
    }
    if (maxZoom >= 5.0f) {
        stops.add(5.0f)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            stops.forEach { stop ->
                val isSelected = abs(currentZoom - stop) < 0.25f

                val textColor by animateColorAsState(
                    targetValue = if (isSelected) LumoraFocusYellow else Color.White.copy(alpha = 0.85f),
                    animationSpec = spring(),
                    label = "zoomTextColor"
                )

                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White.copy(alpha = 0.16f) else Color.Transparent,
                    animationSpec = spring(),
                    label = "zoomBgColor"
                )

                val label = if (stop == 0.5f) ".5" else "${stop.toInt()}"

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(bgColor)
                        .clickable { onZoomSelected(stop) }
                        .testTag("zoom_${label}x"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isSelected) "${String.format("%.1f", currentZoom)}x" else "${label}x",
                        color = textColor,
                        fontSize = if (isSelected && currentZoom != stop) 11.sp else 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}
