package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camera.model.AiSceneResult
import com.example.camera.model.AspectRatioOption
import com.example.camera.model.FlashModeOption
import com.example.camera.model.TimerOption
import com.example.ui.theme.LumoraFocusYellow
import com.example.ui.theme.LumoraGold
import com.example.ui.theme.LumoraWhite
import com.example.ui.theme.LumoraWhite80

@Composable
fun CameraTopBar(
    flashMode: FlashModeOption,
    onFlashToggle: () -> Unit,
    aspectRatio: AspectRatioOption,
    onAspectRatioToggle: () -> Unit,
    timer: TimerOption,
    onTimerToggle: () -> Unit,
    aiDirectorEnabled: Boolean,
    aiSceneResult: AiSceneResult,
    onApplyAiRecommendation: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Icon Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flash Button
            IconButton(
                onClick = onFlashToggle,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .testTag("flash_toggle_button")
            ) {
                val (flashIcon, flashColor) = when (flashMode) {
                    FlashModeOption.AUTO -> Pair(Icons.Default.FlashAuto, LumoraWhite)
                    FlashModeOption.ON -> Pair(Icons.Default.FlashOn, LumoraFocusYellow)
                    FlashModeOption.OFF -> Pair(Icons.Default.FlashOff, LumoraWhite.copy(alpha = 0.5f))
                }
                Icon(
                    imageVector = flashIcon,
                    contentDescription = "Flash mode",
                    tint = flashColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Aspect Ratio Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                    .clickable { onAspectRatioToggle() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("aspect_ratio_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = aspectRatio.label,
                    color = LumoraWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Timer Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .border(
                        1.dp,
                        if (timer != TimerOption.OFF) LumoraFocusYellow else Color.White.copy(alpha = 0.15f),
                        RoundedCornerShape(16.dp)
                    )
                    .clickable { onTimerToggle() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("timer_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Timer",
                        tint = if (timer != TimerOption.OFF) LumoraFocusYellow else LumoraWhite,
                        modifier = Modifier.size(16.dp)
                    )
                    if (timer != TimerOption.OFF) {
                        Text(
                            text = timer.label,
                            color = LumoraFocusYellow,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Settings Button
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .testTag("settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = LumoraWhite,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // On-Device AI Camera Director Floating Tip
        AnimatedVisibility(
            visible = aiDirectorEnabled,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.68f))
                    .border(1.dp, LumoraGold.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                    .clickable { onApplyAiRecommendation() }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .testTag("ai_director_pill"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = aiSceneResult.category.icon,
                        fontSize = 13.sp
                    )
                    Text(
                        text = aiSceneResult.directorTip,
                        color = LumoraWhite80,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (aiSceneResult.recommendedProfile != null) {
                        Text(
                            text = "• APPLY",
                            color = LumoraFocusYellow,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
