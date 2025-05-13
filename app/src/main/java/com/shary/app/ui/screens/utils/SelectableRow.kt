package com.shary.app.ui.screens.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun <T> SelectableRow(
    item: T,
    index: Int,
    isSelected: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    content: @Composable (T) -> Unit
) {
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.secondary
        index % 2 == 0 -> MaterialTheme.colorScheme.surface
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isSelected) }
            .background(backgroundColor)
    ) {
        // Remove the checkbox
        content(item)
    }
}