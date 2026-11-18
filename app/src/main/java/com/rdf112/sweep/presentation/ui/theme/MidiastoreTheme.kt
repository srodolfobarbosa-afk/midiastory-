package com.rdf112.sweep.presentation.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object MidiastoreColors {
    val OledBlack = Color(0xFF000000)
    val ElevatedBg = Color(0xFF000000)
    val CardSurface = Color(0xFF1C1C1E)
    val CardSurfaceAlt = Color(0xFF2C2C2E)
    val CardStroke = Color(0xFF2C2C2E)
    val TextPrimary = Color(0xFFF5F5F7)
    val TextMuted = Color(0xFFA1A1AA)
    val TextSubtle = Color(0xFF71717A)
    val AccentBlue = Color(0xFF0A84FF)
    val AccentBlueSoft = Color(0x290A84FF)
    val DangerRed = Color(0xFFFF453A)
    val GreenSegment = Color(0xFF30D158)
    val GreenSoft = Color(0x2930D158)
    val PurpleSegment = Color(0xFFBF5AF2)
    val AmberSegment = Color(0xFFFF9F0A)
}

private val MidiastoreColorScheme = darkColorScheme(
    primary = MidiastoreColors.AccentBlue,
    onPrimary = Color.White,
    background = MidiastoreColors.OledBlack,
    onBackground = MidiastoreColors.TextPrimary,
    surface = MidiastoreColors.CardSurface,
    onSurface = MidiastoreColors.TextPrimary,
    surfaceVariant = MidiastoreColors.CardSurfaceAlt,
    onSurfaceVariant = MidiastoreColors.TextMuted,
    error = MidiastoreColors.DangerRed,
    onError = Color.White,
)

private val MidiastoreTypography = Typography(
    headlineLarge = TextStyle(
        fontSize = 28.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.5).sp,
    ),
    titleLarge = TextStyle(
        fontSize = 20.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.3).sp,
    ),
    titleMedium = TextStyle(
        fontSize = 15.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    bodyMedium = TextStyle(
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Normal,
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.SemiBold,
    ),
)

@Composable
fun MidiastoreTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MidiastoreColorScheme,
        typography = MidiastoreTypography,
        content = content,
    )
}
