package com.atlas.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    secondary = Accent,
    background = Background,
    surface = Background,
    onPrimary = Background,
    onSecondary = Background,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
)

@Composable
fun AtlasTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
