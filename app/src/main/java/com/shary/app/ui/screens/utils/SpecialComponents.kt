package com.shary.app.ui.screens.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

object SpecialComponents {
    @Composable
    fun CompactActionButton(
        onClick: () -> Unit,
        icon: ImageVector,
        backgroundColor: Color = MaterialTheme.colorScheme.secondary,
        contentDescription: String,
        iconShape: Shape = CircleShape,
        enabled: Boolean = true
    ) {

        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.Companion
                .size(44.dp) // círculo pequeño
                .background(
                    color = if (enabled) backgroundColor else Color.Companion.Gray,
                    shape = iconShape
                )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (enabled) Color.Companion.White else Color.Companion.LightGray,
                modifier = Modifier.Companion.size(28.dp) // icono más grande dentro
            )
        }
    }
}