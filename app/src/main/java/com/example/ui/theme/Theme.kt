package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ViceCoastColorScheme =
  darkColorScheme(
    primary = SunsetCoral,
    onPrimary = ObsidianBg,
    primaryContainer = AsphaltElevated,
    onPrimaryContainer = TextPrimary,
    secondary = NeonCyan,
    onSecondary = ObsidianBg,
    secondaryContainer = AsphaltCard,
    onSecondaryContainer = TextPrimary,
    tertiary = AmberGold,
    onTertiary = ObsidianBg,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = AsphaltSurface,
    onSurface = TextPrimary,
    surfaceVariant = AsphaltCard,
    onSurfaceVariant = TextSecondary,
    outline = OutlineSubtle,
    error = WantedCrimson,
    onError = TextPrimary,
  )

@Composable
fun ViceCoastTheme(content: @Composable () -> Unit) {
  MaterialTheme(colorScheme = ViceCoastColorScheme, typography = Typography, content = content)
}
