package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camera.model.FilmProfile
import com.example.ui.theme.LumoraFocusYellow
import com.example.ui.theme.LumoraGold
import com.example.ui.theme.LumoraWhite80

@Composable
fun FilmProfileDock(
    selectedProfile: FilmProfile,
    onProfileSelected: (FilmProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Tagline of currently active film profile
        Text(
            text = selectedProfile.tagline,
            color = LumoraWhite80,
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Horizontal film strip dock
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilmProfile.entries.forEach { profile ->
                val isSelected = profile == selectedProfile

                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) LumoraFocusYellow else Color.White.copy(alpha = 0.15f),
                    animationSpec = spring(),
                    label = "border"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) LumoraFocusYellow else Color.White.copy(alpha = 0.75f),
                    animationSpec = spring(),
                    label = "text"
                )
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.55f),
                    animationSpec = spring(),
                    label = "bg"
                )

                // Swatch indicator color preview
                val swatchGradient = when (profile) {
                    FilmProfile.NATURAL -> Brush.linearGradient(listOf(Color(0xFF64B5F6), Color(0xFF81C784)))
                    FilmProfile.SOFT_FILM -> Brush.linearGradient(listOf(Color(0xFFFFCCBC), Color(0xFFFFF9C4)))
                    FilmProfile.WARM_FILM -> Brush.linearGradient(listOf(Color(0xFFFFB74D), Color(0xFFFF7043)))
                    FilmProfile.COOL_FILM -> Brush.linearGradient(listOf(Color(0xFF80DEEA), Color(0xFF26A69A)))
                    FilmProfile.CINEMATIC -> Brush.linearGradient(listOf(Color(0xFF00838F), Color(0xFFFF8A65)))
                    FilmProfile.CLASSIC -> Brush.linearGradient(listOf(Color(0xFFEEEEEE), Color(0xFF212121)))
                    FilmProfile.MOODY -> Brush.linearGradient(listOf(Color(0xFF37474F), Color(0xFF263238)))
                    FilmProfile.PORTRAIT_NATURAL -> Brush.linearGradient(listOf(Color(0xFFFFAB91), Color(0xFFFFE082)))
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(bgColor)
                        .border(1.dp, borderColor, RoundedCornerShape(18.dp))
                        .clickable { onProfileSelected(profile) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("film_profile_${profile.id}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(swatchGradient)
                    )

                    Text(
                        text = profile.displayName,
                        color = textColor,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}
