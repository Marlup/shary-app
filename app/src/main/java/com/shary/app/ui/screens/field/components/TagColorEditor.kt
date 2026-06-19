package com.shary.app.ui.screens.field.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shary.app.ui.theme.SharyRadius
import com.shary.app.ui.theme.Violet200
import com.shary.app.ui.theme.Violet600

@Composable
fun TagColorEditor(
    color: Color,
    onColorChanged: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    var hexInput by remember(color) { mutableStateOf(color.toHexRgb()) }
    var hexError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SurfacePreview(color = color)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Color preview",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        ColorSlider(
            label = "R",
            value = color.red * 255f,
            onValueChange = { onColorChanged(color.copy(red = it / 255f)) }
        )
        ColorSlider(
            label = "G",
            value = color.green * 255f,
            onValueChange = { onColorChanged(color.copy(green = it / 255f)) }
        )
        ColorSlider(
            label = "B",
            value = color.blue * 255f,
            onValueChange = { onColorChanged(color.copy(blue = it / 255f)) }
        )

        OutlinedTextField(
            value = hexInput,
            onValueChange = {
                hexInput = it
                val parsed = parseHexColor(it)
                if (parsed != null) {
                    hexError = null
                    onColorChanged(parsed)
                } else {
                    hexError = "Use #RRGGBB or #AARRGGBB"
                }
            },
            label = { Text("Hex color") },
            singleLine = true,
            supportingText = {
                Text(
                    text = hexError ?: "Example: #4A90E2",
                    color = if (hexError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            shape = SharyRadius.input,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Violet600,
                unfocusedBorderColor = Violet200
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SurfacePreview(color: Color) {
    Surface(
        modifier = Modifier.size(24.dp),
        shape = CircleShape,
        color = color,
        border = BorderStroke(1.dp, Violet200)
    ) {}
}

@Composable
private fun ColorSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.labelMedium)
            Text(text = value.toInt().toString(), style = MaterialTheme.typography.labelMedium)
        }
        Slider(
            value = value.coerceIn(0f, 255f),
            onValueChange = onValueChange,
            valueRange = 0f..255f,
            steps = 254,
            modifier = Modifier.height(28.dp)
        )
    }
}

private fun Color.toHexRgb(): String {
    val r = (red * 255).toInt().coerceIn(0, 255)
    val g = (green * 255).toInt().coerceIn(0, 255)
    val b = (blue * 255).toInt().coerceIn(0, 255)
    return String.format("#%02X%02X%02X", r, g, b)
}

private fun parseHexColor(input: String): Color? {
    val hex = input.trim().removePrefix("#")
    return when (hex.length) {
        6 -> {
            val rgb = hex.toLongOrNull(16) ?: return null
            val r = ((rgb shr 16) and 0xFF).toInt()
            val g = ((rgb shr 8) and 0xFF).toInt()
            val b = (rgb and 0xFF).toInt()
            Color(r, g, b)
        }
        8 -> {
            val argb = hex.toLongOrNull(16) ?: return null
            val a = ((argb shr 24) and 0xFF).toInt()
            val r = ((argb shr 16) and 0xFF).toInt()
            val g = ((argb shr 8) and 0xFF).toInt()
            val b = (argb and 0xFF).toInt()
            Color(r, g, b, a)
        }
        else -> null
    }
}
