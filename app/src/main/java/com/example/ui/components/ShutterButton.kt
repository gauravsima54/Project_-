package com.example.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.camera.model.CameraMode
import com.example.ui.theme.LumoraShutterRed
import com.example.ui.theme.LumoraWhite

@Composable
fun ShutterButton(
    cameraMode: CameraMode,
    isRecording: Boolean,
    isCapturing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val isVideoMode = cameraMode == CameraMode.VIDEO

    // Scale animation on press
    val scale by animateFloatAsState(
        targetValue = if (isPressed || isCapturing) 0.90f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "shutterScale"
    )

    // Inner shape corner radius (circle for photo/idle, square for recording)
    val innerCornerRadius by animateDpAsState(
        targetValue = if (isVideoMode && isRecording) 8.dp else 36.dp,
        animationSpec = spring(),
        label = "shutterRadius"
    )

    val innerSize by animateDpAsState(
        targetValue = if (isVideoMode && isRecording) 32.dp else 64.dp,
        animationSpec = spring(),
        label = "shutterInnerSize"
    )

    val innerColor = if (isVideoMode) LumoraShutterRed else LumoraWhite

    Box(
        modifier = modifier
            .size(82.dp)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onClick()
            }
            .testTag("shutter_button"),
        contentAlignment = Alignment.Center
    ) {
        // Outer concentric ring
        Box(
            modifier = Modifier
                .size(80.dp)
                .border(
                    width = 4.dp,
                    color = if (isVideoMode && isRecording) LumoraShutterRed.copy(alpha = 0.5f) else Color.White,
                    shape = CircleShape
                )
        )

        // Inner trigger button
        Box(
            modifier = Modifier
                .size(innerSize)
                .clip(RoundedCornerShape(innerCornerRadius))
                .background(innerColor)
        )
    }
}
