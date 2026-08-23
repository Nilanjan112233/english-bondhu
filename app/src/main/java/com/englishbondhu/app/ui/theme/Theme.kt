package com.englishbondhu.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF155E54),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6F4EC),
    onPrimaryContainer = Color(0xFF073B34),
    secondary = Color(0xFFF28C52),
    secondaryContainer = Color(0xFFFFE2CF),
    background = Color(0xFFFFF9F0),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF3EDE4),
    error = Color(0xFFB3261E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF85D5C5),
    secondary = Color(0xFFFFB68C),
    background = Color(0xFF101513),
    surface = Color(0xFF171D1B),
)

@Composable
fun EnglishBondhuTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
