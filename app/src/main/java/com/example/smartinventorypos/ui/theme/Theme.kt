package com.example.smartinventorypos.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val Colors = lightColorScheme(
    primary = Yellow,
    onPrimary = Ink,
    primaryContainer = YellowSoft,
    onPrimaryContainer = Brown,
    secondary = Brown,
    onSecondary = Yellow,
    secondaryContainer = YellowSoft,
    onSecondaryContainer = Brown,
    tertiary = Brown,
    onTertiary = Color.White,
    background = Color.White,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Mist,
    onSurfaceVariant = Muted,
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Mist,
    outline = Hairline,
    outlineVariant = Hairline,
    error = Danger,
    onError = Color.White,
    errorContainer = DangerSoft,
    onErrorContainer = Danger
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp)
)

/** The design has one look, so the app is always light and ignores wallpaper-based colors. */
@Composable
fun SmartInventoryPOSTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Colors,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
