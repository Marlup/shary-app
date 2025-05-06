package com.shary.app.ui.screens.ui_utils

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun <T> SelectableRow(
    item: T,
    index: Int,
    isSelected: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    content: @Composable (T) -> Unit
) {
    // +++++ Logic to alternate rows color +++++
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.secondary
        index % 2 == 0 -> MaterialTheme.colorScheme.surface
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    // +++++ Row layout +++++
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(backgroundColor)
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = onCheckedChange
        )
        content(item) // Contenido específico para cada tipo
    }
}
