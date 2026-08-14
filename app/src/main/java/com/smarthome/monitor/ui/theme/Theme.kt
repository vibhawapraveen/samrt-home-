package com.smarthome.monitor.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = SmartBlue,
    onPrimary = Color.White,
    primaryContainer = SurfaceVariantDark,
    onPrimaryContainer = SmartBlueLight,
    secondary = TealAccent,
    onSecondary = BackgroundDark,
    secondaryContainer = Color(0xFF0D3D36),
    onSecondaryContainer = TealAccent,
    tertiary = PurpleAccent,
    onTertiary = Color.White,
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondary,
    error = StateError,
    onError = Color.White,
    outline = BorderSubtle,
    outlineVariant = GlassWhite,
    scrim = Color(0xCC000000)
)

@Composable
fun SmartHomeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AppTypography,
        content = content
    )
}
