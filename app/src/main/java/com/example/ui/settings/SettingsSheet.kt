package com.example.ui.settings

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camera.model.CameraMode
import com.example.camera.model.CameraSettings
import com.example.camera.model.FilmProfile
import com.example.camera.model.GridModeOption
import com.example.camera.model.QualityOption
import com.example.ui.theme.LumoraFocusYellow
import com.example.ui.theme.LumoraGold
import com.example.ui.theme.LumoraSurfaceBorder
import com.example.ui.theme.LumoraSurfaceDark
import com.example.ui.theme.LumoraSurfaceElevated
import com.example.ui.theme.LumoraWhite
import com.example.ui.theme.LumoraWhite50
import com.example.ui.theme.LumoraWhite80

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    settings: CameraSettings,
    onSettingsChanged: (CameraSettings) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = LumoraSurfaceDark,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Camera Settings",
                    color = LumoraWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                        .testTag("settings_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = LumoraWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Group: Capture & Quality
            SettingsSectionHeader("CAPTURE QUALITY")
            SettingsCard {
                QualityOption.entries.forEachIndexed { index, quality ->
                    val isSelected = settings.quality == quality
                    SelectableRow(
                        title = quality.label,
                        isSelected = isSelected,
                        onClick = { onSettingsChanged(settings.copy(quality = quality)) }
                    )
                    if (index < QualityOption.entries.size - 1) {
                        HorizontalDivider(color = LumoraSurfaceBorder, thickness = 0.5.dp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Group: Composition & Assists
            SettingsSectionHeader("COMPOSITION GUIDES")
            SettingsCard {
                GridModeOption.entries.forEachIndexed { index, grid ->
                    val isSelected = settings.gridMode == grid
                    SelectableRow(
                        title = grid.label,
                        isSelected = isSelected,
                        onClick = { onSettingsChanged(settings.copy(gridMode = grid)) }
                    )
                    if (index < GridModeOption.entries.size - 1) {
                        HorizontalDivider(color = LumoraSurfaceBorder, thickness = 0.5.dp)
                    }
                }

                HorizontalDivider(color = LumoraSurfaceBorder, thickness = 0.5.dp)

                ToggleRow(
                    title = "Horizon Leveler",
                    subtitle = "Shows tilt angle with tactile level feedback",
                    isChecked = settings.levelerEnabled,
                    onCheckedChange = { onSettingsChanged(settings.copy(levelerEnabled = it)) }
                )

                HorizontalDivider(color = LumoraSurfaceBorder, thickness = 0.5.dp)

                ToggleRow(
                    title = "Tactile Haptics",
                    subtitle = "Vibrate subtly on shutter and horizon lock",
                    isChecked = settings.hapticEnabled,
                    onCheckedChange = { onSettingsChanged(settings.copy(hapticEnabled = it)) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Group: AI Director
            SettingsSectionHeader("ON-DEVICE AI DIRECTOR")
            SettingsCard {
                ToggleRow(
                    title = "AI Scene Director",
                    subtitle = "Real-time on-device suggestions for lighting, skin tones, and composition",
                    isChecked = settings.aiDirectorEnabled,
                    onCheckedChange = { onSettingsChanged(settings.copy(aiDirectorEnabled = it)) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Group: Privacy & Architecture
            SettingsSectionHeader("PRIVACY & OFFLINE ARCHITECTURE")
            SettingsCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(LumoraGold.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = LumoraGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "100% Private & Local",
                            color = LumoraWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Photographs are stored only on your local device. Scene recognition runs fully offline with zero cloud server telemetry.",
                            color = LumoraWhite50,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // About Lumora
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "LUMORA AI CAMERA",
                    color = LumoraWhite80,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Version 1.0 • Apple Minimalist Camera Engine",
                    color = LumoraWhite50,
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        color = LumoraWhite50,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = 6.dp, bottom = 6.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(LumoraSurfaceElevated)
            .border(1.dp, LumoraSurfaceBorder, RoundedCornerShape(14.dp))
    ) {
        Column {
            content()
        }
    }
}

@Composable
private fun SelectableRow(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = if (isSelected) LumoraFocusYellow else LumoraWhite,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(LumoraFocusYellow)
            )
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                color = LumoraWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                color = LumoraWhite50,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }

        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = LumoraFocusYellow,
                uncheckedThumbColor = LumoraWhite80,
                uncheckedTrackColor = LumoraSurfaceBorder
            )
        )
    }
}
