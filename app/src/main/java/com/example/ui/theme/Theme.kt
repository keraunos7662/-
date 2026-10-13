package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val CrimeDarkColorScheme = darkColorScheme(
    primary = CrimeGold,
    onPrimary = CrimeBgDark,
    primaryContainer = CrimeGoldDark,
    onPrimaryContainer = CrimeGoldLight,

    secondary = CrimeCrimson,
    onSecondary = CrimeTextPrimary,
    secondaryContainer = CrimeCrimsonDark,
    onSecondaryContainer = CrimeCrimsonLight,

    tertiary = CrimePoliceBlue,
    onTertiary = CrimeTextPrimary,

    background = CrimeBgDark,
    onBackground = CrimeTextPrimary,

    surface = CrimeSurfaceDark,
    onSurface = CrimeTextPrimary,
    surfaceVariant = CrimeSurfaceCard,
    onSurfaceVariant = CrimeTextSecondary,

    error = CrimeHeatRed,
    onError = CrimeTextPrimary,

    outline = CrimeBorder,
    outlineVariant = CrimeSurfaceHighlight
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = CrimeDarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = CrimeBgDark.toArgb()
                window.navigationBarColor = CrimeBgDark.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = false
                insetsController.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
