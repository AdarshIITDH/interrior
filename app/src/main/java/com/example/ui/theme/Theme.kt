package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VisionSpaceDarkColorScheme = darkColorScheme(
    primary = CyanNeon,
    onPrimary = Color.Black,
    primaryContainer = ObsidianSurfaceVariant,
    onPrimaryContainer = CyanNeon,
    secondary = CyanSubtle,
    onSecondary = Color.Black,
    secondaryContainer = ObsidianSurface,
    onSecondaryContainer = TextPrimary,
    tertiary = ElectricBlue,
    background = ObsidianBackground,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = ObsidianCardBorder,
    outlineVariant = GlassBorder,
    error = CrimsonAlert,
    onError = Color.White
)

@Composable
fun VisionSpaceTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = VisionSpaceDarkColorScheme,
        typography = Typography,
        content = content
    )
}
