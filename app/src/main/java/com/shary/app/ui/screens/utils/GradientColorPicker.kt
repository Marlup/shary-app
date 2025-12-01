package com.shary.app.ui.screens.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun GradientColorPicker(
    onColorSelected: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(
                Brush.horizontalGradient(colors)
            )
            .clickable { /* no-op, handled by detectTapGestures */ },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val fraction = offset.x / size.width
                        val picked = lerpColors(colors, fraction)
                        onColorSelected(picked)
                    }
                }
        )
    }
}

// Helper: interpolate between gradient stops
fun lerpColors(colors: List<Color>, fraction: Float): Color {
    val step = 1f / (colors.size - 1)
    val index = (fraction / step).toInt().coerceIn(0, colors.size - 2)
    val localT = (fraction - index * step) / step
    return androidx.compose.ui.graphics.lerp(colors[index], colors[index + 1], localT)
}
