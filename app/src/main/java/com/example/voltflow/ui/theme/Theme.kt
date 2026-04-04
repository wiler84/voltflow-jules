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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Brush
import com.example.voltflow.ui.LocalVoltflowDesign
import com.example.voltflow.ui.VoltflowDesignColors

private val LightColors = lightColorScheme(
    primary = Color(0xFF2F7FCF),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF4A9AE8),
    onPrimaryContainer = Color(0xFFFFFFFF),

    secondary = Color(0xFF56B4E7),
    onSecondary = VoltDarkText,
    secondaryContainer = Color(0xFFE7F2FF),
    onSecondaryContainer = VoltDarkText,

    tertiary = Color(0xFF3FB58D),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE9F8F2),
    onTertiaryContainer = VoltDarkText,

    background = Color(0xFFEEF3F8),
    onBackground = Color(0xFF1A2433),

    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A2433),

    surfaceVariant = Color(0xFFF4F7FB),
    onSurfaceVariant = Color(0xFF79869A),

    outline = Color(0xFFDCE4EF),

    error = VoltDanger,
    onError = Color.White,
    errorContainer = VoltDanger.copy(alpha = 0.2f),
    onErrorContainer = VoltDarkText,
)

private val DarkColors = darkColorScheme(
    primary = VoltBlue,
    onPrimary = VoltInk,
    primaryContainer = VoltBlue,
    onPrimaryContainer = VoltInk,

    secondary = VoltTeal,
    onSecondary = VoltInk,
    secondaryContainer = VoltTeal,
    onSecondaryContainer = VoltInk,

    tertiary = Color(0xFF7FA8FF),
    onTertiary = VoltInk,
    tertiaryContainer = Color(0xFF7FA8FF),
    onTertiaryContainer = VoltInk,

    background = VoltNight,
    onBackground = VoltInk,

    surface = VoltNightSurface,
    onSurface = VoltInk,

    surfaceVariant = VoltNightElevated,
    onSurfaceVariant = VoltSlate,

    outline = Color(0xFF2C364B),

    error = VoltDanger,
    onError = Color.White,
    errorContainer = VoltDanger.copy(alpha = 0.2f),
    onErrorContainer = VoltInk,
)


@Composable
fun VoltflowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    // Define custom VoltflowDesignColors based on MaterialTheme.colorScheme and specific needs
    val customColors = if (darkTheme) {
        VoltflowDesignColors(
            bgTop = Color(0xFF0F172A),
            bgBottom = Color(0xFF020617),
            cardBg = Color(0xFF1E293B).copy(alpha = 0.6f),
            cardBgSolid = Color(0xFF1E293B),
            iconCircleBg = Color(0xFF334155).copy(alpha = 0.5f),
            grayText = Color(0xFF94A3B8),
            blueAccent = Color(0xFF3B82F6),
            destructiveRed = Color(0xFFEF4444),
            destructiveBg = Color(0x1AEF4444),
            headerCircle = Color(0xFF1E293B),
            bottomNavBg = Color(0xFF1E293B).copy(alpha = 0.7f),
            warningAmber = Color(0xFFF59E0B),
            inputBg = Color(0xFF0F172A).copy(alpha = 0.5f),
            modalBg = Color(0xFF0F172A),
            dividerColor = Color(0x1FFFFFFF),
            primaryGradient = Brush.horizontalGradient(listOf(Color(0xFF3B82F6), Color(0xFF2563EB))),
            logoGradient = Brush.verticalGradient(listOf(Color(0xFF60A5FA), Color(0xFF3B82F6))),
        )
    } else {
        // Light theme custom colors - map to MaterialTheme colors for consistency
        VoltflowDesignColors(
            bgTop = Color(0xFFEDF2F8),
            bgBottom = Color(0xFFEEF3F8),
            cardBg = Color(0xFFFFFFFF).copy(alpha = 0.95f),
            cardBgSolid = Color(0xFFFFFFFF),
            iconCircleBg = Color(0xFFF1F5FA),
            grayText = Color(0xFF7D8899),
            blueAccent = colorScheme.primary,
            destructiveRed = colorScheme.error,
            destructiveBg = colorScheme.error.copy(alpha = 0.1f),
            headerCircle = Color(0xFFF4F7FB),
            bottomNavBg = Color(0xFFFFFFFF).copy(alpha = 0.78f),
            warningAmber = Color(0xFFD9B24C),
            inputBg = Color(0xFFF2F5F9),
            modalBg = Color(0xFFFFFFFF),
            dividerColor = Color(0xFFE9EEF5),
            primaryGradient = Brush.horizontalGradient(listOf(Color(0xFF2F7FCF), Color(0xFF4A9AE8))),
            logoGradient = Brush.verticalGradient(listOf(Color(0xFF4A9AE8), Color(0xFF2F7FCF))),
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalVoltflowDesign provides customColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}
