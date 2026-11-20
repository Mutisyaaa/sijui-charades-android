package com.sijui.charades.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.sijui.charades.ui.design.Sj

private val SijuiNightScheme = darkColorScheme(
    primary = Sj.Gold,
    onPrimary = Sj.Night,
    secondary = Sj.Orchid,
    onSecondary = Color.White,
    tertiary = Sj.Savanna,
    onTertiary = Sj.Night,
    error = Sj.Ember,
    onError = Color.White,
    background = Sj.Night,
    onBackground = Sj.TextHi,
    surface = Sj.Night2,
    onSurface = Sj.TextHi,
    surfaceVariant = Sj.Night3,
    onSurfaceVariant = Sj.TextMid,
    surfaceContainerHigh = Sj.Night3,
    outline = Color(0x40FFFFFF),
)

@Composable
fun SijuiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SijuiNightScheme,
        typography = SijuiTypography,
        content = content,
    )
}
