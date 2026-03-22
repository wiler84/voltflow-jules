package com.example.voltflow.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = VoltBlue,
    secondary = VoltTeal,
    tertiary = Color(0xFF7FA8FF),
    background = Color(0xFFF6F7FB),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE9EDF6),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFFD6DEEB),
)

private val DarkColors = darkColorScheme(
    primary = VoltBlue,
    secondary = VoltTeal,
    tertiary = Color(0xFF7FA8FF),
    background = VoltNight,
    surface = VoltNightSurface,
    surfaceVariant = VoltNightElevated,
    onPrimary = Color(0xFF0B0F1A),
    onSecondary = Color(0xFF0B0F1A),
    onBackground = VoltInk,
    onSurface = VoltInk,
    onSurfaceVariant = VoltSlate,
    outline = Color(0xFF2C364B),
)

@Composable
fun VoltflowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
