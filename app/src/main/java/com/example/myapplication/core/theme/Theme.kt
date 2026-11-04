package com.example.myapplication.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Colours taken from the Figma design
val DormGreen = Color(0xFF2E5E45)
val DormGreenLight = Color(0xFFEAF2EC)
val CardGrey = Color(0xFFF3F3F0)
val TextDark = Color(0xFF17201B)
val TextMuted = Color(0xFF5F6A63)
val BorderGrey = Color(0xFFD9DDD8)

private val LightColors = lightColorScheme(
    primary = DormGreen,
    onPrimary = Color.White,
    primaryContainer = DormGreenLight,
    onPrimaryContainer = DormGreen,
    secondaryContainer = DormGreenLight,
    onSecondaryContainer = DormGreen,
    background = Color.White,
    onBackground = TextDark,
    surface = Color.White,
    onSurface = TextDark,
    surfaceVariant = CardGrey,
    onSurfaceVariant = TextMuted,
    outline = BorderGrey
)

@Composable
fun MyApplicationTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}
