package com.shary.app.ui.screens.utils

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun FilterBox(
    title: String,
    onClick: () -> Unit,
    width: Int = 45,
    height: Int = 45
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.primary,
        contentColor = Color.White,
        shape = MaterialTheme.shapes.medium,
        shadowElevation = 4.dp,
        modifier = Modifier
            .size(width.dp, height.dp)
            .fillMaxWidth(0.15f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
