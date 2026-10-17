package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val LumoraDarkColorScheme = darkColorScheme(
    primary = LumoraGold,
    onPrimary = LumoraBlack,
    primaryContainer = LumoraSurfaceElevated,
    onPrimaryContainer = LumoraWhite,
    secondary = LumoraAmber,
    onSecondary = LumoraBlack,
    surface = LumoraObsidian,
    onSurface = LumoraWhite,
    surfaceVariant = LumoraSurfaceDark,
    onSurfaceVariant = LumoraWhite80,
    background = LumoraBlack,
    onBackground = LumoraWhite,
    error = LumoraShutterRed,
    onError = LumoraWhite
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LumoraDarkColorScheme,
        typography = Typography,
        content = content
    )
}
