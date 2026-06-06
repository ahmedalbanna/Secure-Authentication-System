package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PolishColorScheme = lightColorScheme(
    primary = PolishBrandBlue,
    onPrimary = Color.White,
    secondary = PolishCardGray,
    onSecondary = PolishDarkText,
    tertiary = Color(0xFF10B981),
    background = PolishBg,
    onBackground = PolishDarkText,
    surface = PolishWhite,
    onSurface = PolishDarkText,
    outline = PolishBorder
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PolishColorScheme,
        typography = Typography,
        content = content
    )
}
