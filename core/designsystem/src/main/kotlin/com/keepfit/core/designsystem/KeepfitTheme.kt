package com.keepfit.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val KeepfitColors = lightColorScheme(
    primary = Color(0xFF196B4A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE9E1),
    onPrimaryContainer = Color(0xFF123C2D),
    secondary = Color(0xFFD9772B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE7D2),
    onSecondaryContainer = Color(0xFF5E2D08),
    tertiary = Color(0xFF496456),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE7ECE8),
    onTertiaryContainer = Color(0xFF24382E),
    background = Color(0xFFF7F8F3),
    onBackground = Color(0xFF18201B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF18201B),
    surfaceVariant = Color(0xFFE7ECE8),
    onSurfaceVariant = Color(0xFF59635D),
    outline = Color(0xFFB7C0BA),
    outlineVariant = Color(0xFFD6DDD8),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1F4EF),
    surfaceContainer = Color(0xFFEBEFE9),
    surfaceContainerHigh = Color(0xFFE4EAE4),
    error = Color(0xFFB23A35),
)

private val KeepfitTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 38.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.7).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.35).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 31.sp,
        letterSpacing = (-0.2).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 27.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    ),
)

private val KeepfitShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun KeepfitTheme(
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    KeepfitMotionProvider(reduceMotion = reduceMotion) {
        MaterialTheme(
            colorScheme = KeepfitColors,
            typography = KeepfitTypography,
            shapes = KeepfitShapes,
            content = content,
        )
    }
}
