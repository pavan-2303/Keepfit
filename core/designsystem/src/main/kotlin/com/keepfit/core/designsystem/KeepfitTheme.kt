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
    primary = Color(0xFF246B4F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8ECDD),
    onPrimaryContainer = Color(0xFF113C2B),
    secondary = Color(0xFFB65C18),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE1CC),
    onSecondaryContainer = Color(0xFF572500),
    background = Color(0xFFF7F8F4),
    onBackground = Color(0xFF1A211E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A211E),
    surfaceVariant = Color(0xFFE8ECE7),
    onSurfaceVariant = Color(0xFF58635E),
    outline = Color(0xFFB8C1BC),
    error = Color(0xFFB3261E),
)

private val KeepfitTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 38.sp,
        lineHeight = 42.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
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
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(8.dp),
)

@Composable
fun KeepfitTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KeepfitColors,
        typography = KeepfitTypography,
        shapes = KeepfitShapes,
        content = content,
    )
}

