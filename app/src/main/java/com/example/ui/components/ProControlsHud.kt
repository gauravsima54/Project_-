package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LumoraFocusYellow
import com.example.ui.theme.LumoraWhite50
import com.example.ui.theme.LumoraWhite80

@Composable
fun ProControlsHud(
    exposureOffset: Float, // -2.0 to +2.0
    onExposureChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.65f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag("pro_controls_hud")
    ) {
        Column {
            // Readout row: ISO, SHUTTER, EV, WB
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HudMetric(label = "EV", value = if (exposureOffset > 0) "+%.1f".format(exposureOffset) else "%.1f".format(exposureOffset), isHighlighted = true)
                HudMetric(label = "ISO", value = "AUTO (125)")
                HudMetric(label = "SEC", value = "1/250s")
                HudMetric(label = "WB", value = "5400K")
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Exposure Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "-2.0",
                    color = LumoraWhite50,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )

                Slider(
                    value = exposureOffset,
                    onValueChange = onExposureChanged,
                    valueRange = -2.0f..2.0f,
                    steps = 19, // 0.2 increments
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                        .testTag("pro_ev_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = LumoraFocusYellow,
                        activeTrackColor = LumoraFocusYellow,
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    )
                )

                Text(
                    text = "+2.0",
                    color = LumoraWhite50,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun HudMetric(
    label: String,
    value: String,
    isHighlighted: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = LumoraWhite50,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Text(
            text = value,
            color = if (isHighlighted) LumoraFocusYellow else LumoraWhite80,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
    }
}
