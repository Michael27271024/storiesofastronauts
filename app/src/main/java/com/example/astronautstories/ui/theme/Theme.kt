package com.example.astronautstories.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val CosmicDarkColorScheme = darkColorScheme(
    primary = CosmicPrimary,
    onPrimary = CosmicOnPrimary,
    primaryContainer = CosmicPrimaryContainer,
    onPrimaryContainer = CosmicOnBackground,
    secondary = CosmicSecondary,
    onSecondary = CosmicOnSecondary,
    secondaryContainer = CosmicSecondaryContainer,
    onSecondaryContainer = CosmicSecondary,
    tertiary = CosmicTertiary,
    background = CosmicBackground,
    onBackground = CosmicOnBackground,
    surface = CosmicSurface,
    onSurface = CosmicOnBackground,
    surfaceVariant = CosmicSurfaceVariant,
    onSurfaceVariant = CosmicOnSurfaceVariant,
    outline = CosmicOutline,
    error = CosmicError,
    onError = Color.White
)

private val SpaceLightColorScheme = lightColorScheme(
    primary = SpaceLightPrimary,
    onPrimary = SpaceLightOnPrimary,
    primaryContainer = SpaceLightPrimaryContainer,
    onPrimaryContainer = SpaceLightOnBackground,
    secondary = SpaceLightSecondary,
    onSecondary = SpaceLightOnSecondary,
    secondaryContainer = SpaceLightSecondaryContainer,
    onSecondaryContainer = SpaceLightOnBackground,
    tertiary = SpaceLightTertiary,
    background = SpaceLightBackground,
    onBackground = SpaceLightOnBackground,
    surface = SpaceLightSurface,
    onSurface = SpaceLightOnBackground,
    surfaceVariant = SpaceLightSurfaceVariant,
    onSurfaceVariant = SpaceLightOnSurfaceVariant,
    outline = SpaceLightOutline,
    error = SpaceLightError,
    onError = Color.White
)

private val HighContrastDarkColorScheme = CosmicDarkColorScheme.copy(
    background = VoidBlack,
    surface = Color(0xFF0E0E12),
    surfaceVariant = Color(0xFF1A1816),
    primary = Color(0xFFE89B54),
    secondary = Color(0xFFFFF1D6),
    onBackground = Color(0xFFFFF6E5),
    onSurface = Color(0xFFFFF6E5),
    outline = Color(0xFFE89B54)
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun AstronautStoriesTheme(
    darkTheme: Boolean = true,
    accessibilityHighContrast: Boolean = false,
    largeTextScale: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        accessibilityHighContrast -> HighContrastDarkColorScheme
        darkTheme -> CosmicDarkColorScheme
        else -> SpaceLightColorScheme
    }
    val typography = buildAppTypography(scaleFactor = if (largeTextScale) 1.18f else 1.0f)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        shapes = AppShapes,
        content = content
    )
}
