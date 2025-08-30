package com.teamflux.fluxai.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.BlendMode.Companion.Color
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Flat Design Dark Theme with Black, White, and Pale Blue
private val FluxAIDarkColorScheme = darkColorScheme(
    // Primary colors - Pale Blue
    primary = PaleBlue,
    onPrimary = Black,
    primaryContainer = PaleBlueDark,
    onPrimaryContainer = White,

    // Secondary colors - Soft pale blue
    secondary = PaleBlueSoft,
    onSecondary = Black,
    secondaryContainer = PaleBlueDark,
    onSecondaryContainer = White,

    // Tertiary colors
    tertiary = PaleBlueLight,
    onTertiary = Black,
    tertiaryContainer = PaleBlueDark,
    onTertiaryContainer = White,

    // Background colors
    background = BackgroundDark,
    onBackground = OnBackgroundDark,

    // Surface colors
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,

    // Container colors
    surfaceContainer = SoftBlack,
    surfaceContainerHigh = SurfaceVariantDark,
    surfaceContainerHighest = SurfaceVariantDark,

    // Outline colors
    outline = OnSurfaceVariantDark,
    outlineVariant = SurfaceVariantDark,

    // Error colors (keep minimal for flat design)
    error = PaleBlueDark,
    onError = White,
    errorContainer = SoftBlack,
    onErrorContainer = PaleBlue
)

// Flat Design Light Theme with White, Black, and Pale Blue
private val FluxAILightColorScheme = lightColorScheme(
    // Primary colors - Pale Blue
    primary = PaleBlueDark,
    onPrimary = White,
    primaryContainer = PaleBlueLight,
    onPrimaryContainer = Black,

    // Secondary colors - Soft pale blue
    secondary = PaleBlueSoft,
    onSecondary = White,
    secondaryContainer = PaleBlueLight,
    onSecondaryContainer = Black,

    // Tertiary colors
    tertiary = PaleBlue,
    onTertiary = White,
    tertiaryContainer = PaleBlueLight,
    onTertiaryContainer = Black,

    // Background colors
    background = White,
    onBackground = Black,

    // Surface colors
    surface = Color(0xFFF8F8F8),
    onSurface = Black,
    surfaceVariant = Color(0xFFE8E8E8),
    onSurfaceVariant = Color(0xFF404040),

    // Container colors
    surfaceContainer = Color(0xFFF0F0F0),
    surfaceContainerHigh = Color(0xFFE8E8E8),
    surfaceContainerHighest = Color(0xFFE0E0E0),

    // Outline colors
    outline = Color(0xFFB0B0B0),
    outlineVariant = Color(0xFFD0D0D0),

    // Error colors
    error = PaleBlueDark,
    onError = White,
    errorContainer = PaleBlueLight,
    onErrorContainer = Black
)

@Composable
fun FluxAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disable dynamic colors to maintain our design
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) FluxAIDarkColorScheme else FluxAILightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}