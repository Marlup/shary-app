package com.shary.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SharyLightColorScheme = lightColorScheme(
    primary = Violet600,
    onPrimary = Color.White,
    primaryContainer = TagVioletBg,
    onPrimaryContainer = TagVioletText,
    secondary = Violet500,
    onSecondary = Color.White,
    background = Violet50,
    onBackground = Violet900,
    surface = SurfaceLight,
    onSurface = Violet900,
    surfaceVariant = SurfaceMid,
    onSurfaceVariant = Violet500,
    outline = Violet200,
    outlineVariant = Violet300,
    error = DestructiveText,
    onError = Color.White,
    errorContainer = DestructiveBg,
    onErrorContainer = DestructiveText,
)

@Composable
fun SharyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SharyLightColorScheme,
        typography = SharyTypography,
        shapes = SharyShapes,
        content = content
    )
}
