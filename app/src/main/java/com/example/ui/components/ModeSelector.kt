package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camera.model.CameraMode
import com.example.ui.theme.LumoraFocusYellow
import com.example.ui.theme.LumoraWhite50

@Composable
fun ModeSelector(
    currentMode: CameraMode,
    onModeSelected: (CameraMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CameraMode.entries.forEach { mode ->
            val isSelected = mode == currentMode

            val textColor by animateColorAsState(
                targetValue = if (isSelected) LumoraFocusYellow else LumoraWhite50,
                animationSpec = spring(),
                label = "modeTextColor"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onModeSelected(mode) }
                    .padding(horizontal = 4.dp, vertical = 6.dp)
                    .testTag("mode_${mode.name.lowercase()}")
            ) {
                Text(
                    text = mode.label,
                    color = textColor,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    letterSpacing = 0.8.sp
                )

                Spacer(modifier = Modifier.height(3.dp))

                // Active dot indicator
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(
                            color = if (isSelected) LumoraFocusYellow else Color.Transparent,
                            shape = CircleShape
                        )
                )
            }
        }
    }
}
