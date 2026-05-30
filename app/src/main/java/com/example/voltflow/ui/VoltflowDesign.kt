package com.example.voltflow.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.voltflow.R
import androidx.compose.material3.MaterialTheme

@Immutable
data class VoltflowDesignColors(
    val bgTop: Color,
    val bgBottom: Color,
    val cardBg: Color,
    val cardBgSolid: Color,
    val iconCircleBg: Color,
    val grayText: Color,
    val blueAccent: Color,
    val destructiveRed: Color,
    val destructiveBg: Color,
    val headerCircle: Color,
    val bottomNavBg: Color,
    val warningAmber: Color,
    val inputBg: Color,
    val modalBg: Color,
    val dividerColor: Color,
    val primaryGradient: Brush,
    val logoGradient: Brush,
    val walletGradient: Brush,
)

val LocalVoltflowDesign = staticCompositionLocalOf {
    VoltflowDesignColors(
        bgTop = Color.Unspecified,
        bgBottom = Color.Unspecified,
        cardBg = Color.Unspecified,
        cardBgSolid = Color.Unspecified,
        iconCircleBg = Color.Unspecified,
        grayText = Color.Unspecified,
        blueAccent = Color.Unspecified,
        destructiveRed = Color.Unspecified,
        destructiveBg = Color.Unspecified,
        headerCircle = Color.Unspecified,
        bottomNavBg = Color.Unspecified,
        warningAmber = Color.Unspecified,
        inputBg = Color.Unspecified,
        modalBg = Color.Unspecified,
        dividerColor = Color.Unspecified,
        primaryGradient = Brush.horizontalGradient(listOf(Color.Unspecified, Color.Unspecified)),
        logoGradient = Brush.verticalGradient(listOf(Color.Unspecified, Color.Unspecified)),
        walletGradient = Brush.linearGradient(listOf(Color.Unspecified, Color.Unspecified)),
    )
}

object VoltflowDesign {
    val BgTop: Color
        @Composable get() = LocalVoltflowDesign.current.bgTop
    val BgBottom: Color
        @Composable get() = LocalVoltflowDesign.current.bgBottom
    val CardBg: Color
        @Composable get() = LocalVoltflowDesign.current.cardBg
    val CardBgSolid: Color
        @Composable get() = LocalVoltflowDesign.current.cardBgSolid
    val IconCircleBg: Color
        @Composable get() = LocalVoltflowDesign.current.iconCircleBg
    val GrayText: Color
        @Composable get() = LocalVoltflowDesign.current.grayText
    val BlueAccent: Color
        @Composable get() = LocalVoltflowDesign.current.blueAccent
    val DestructiveRed: Color
        @Composable get() = LocalVoltflowDesign.current.destructiveRed
    val DestructiveBg: Color
        @Composable get() = LocalVoltflowDesign.current.destructiveBg
    val HeaderCircle: Color
        @Composable get() = LocalVoltflowDesign.current.headerCircle
    val BottomNavBg: Color
        @Composable get() = LocalVoltflowDesign.current.bottomNavBg
    val WarningAmber: Color
        @Composable get() = LocalVoltflowDesign.current.warningAmber
    val InputBg: Color
        @Composable get() = LocalVoltflowDesign.current.inputBg
    val ModalBg: Color
        @Composable get() = LocalVoltflowDesign.current.modalBg
    val DividerColor: Color
        @Composable get() = LocalVoltflowDesign.current.dividerColor
    val PrimaryGradient: Brush
        @Composable get() = LocalVoltflowDesign.current.primaryGradient
    val LogoGradient: Brush
        @Composable get() = LocalVoltflowDesign.current.logoGradient
    val WalletGradient: Brush
        @Composable get() = LocalVoltflowDesign.current.walletGradient

    val SoraFont = FontFamily(
        Font(R.font.sora_wght, weight = FontWeight.Normal),
        Font(R.font.sora_wght, weight = FontWeight.SemiBold),
        Font(R.font.sora_wght, weight = FontWeight.Bold),
        Font(R.font.sora_wght, weight = FontWeight.Black)
    )
    
    val ManropeFont = FontFamily(
        Font(R.font.manrope_wght, weight = FontWeight.Normal),
        Font(R.font.manrope_wght, weight = FontWeight.Medium),
        Font(R.font.manrope_wght, weight = FontWeight.SemiBold),
        Font(R.font.manrope_wght, weight = FontWeight.Bold)
    )

    val appFont = SoraFont
    val bodyFont = ManropeFont
}

/**
 * Professional Shimmer effect for skeleton screens.
 */
@Composable
fun Modifier.shimmerLoading(
    isLoading: Boolean,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp)
): Modifier = if (isLoading) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = 0.1f),
        Color.LightGray.copy(alpha = 0.3f),
        Color.LightGray.copy(alpha = 0.1f),
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = androidx.compose.ui.geometry.Offset.Zero,
        end = androidx.compose.ui.geometry.Offset(x = translateAnim, y = translateAnim)
    )

    this.background(brush, shape)
} else {
    this
}
