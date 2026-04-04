package com.example.voltflow.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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

    val AppFont = SoraFont
    val BodyFont = ManropeFont
}
