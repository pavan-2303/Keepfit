package com.keepfit.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

@Immutable
data class KeepfitMotionSettings(
    val reduceMotion: Boolean = false,
)

val LocalKeepfitMotionSettings = staticCompositionLocalOf { KeepfitMotionSettings() }

@Composable
fun KeepfitMotionProvider(
    reduceMotion: Boolean,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalKeepfitMotionSettings provides KeepfitMotionSettings(reduceMotion),
        content = content,
    )
}
