package com.shary.app.ui.screens.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.shary.app.core.domain.types.enums.AppTheme
import com.shary.app.ui.theme.getExtendedColors

object SpecialComponents {
    @Composable
    fun CompactActionButton(
        onClick: () -> Unit,
        icon: ImageVector,
        backgroundColor: Color = MaterialTheme.colorScheme.secondary,
        contentDescription: String,
        iconShape: Shape = CircleShape,
        enabled: Boolean = true,
        theme: AppTheme = AppTheme.Pastel,
        useExtendedColors: Boolean = true
    ) {
        val extendedColors = getExtendedColors(theme = theme)

        val bgColor = if (useExtendedColors) {
            if (enabled) extendedColors.accent else Color.Gray
        } else {
            if (enabled) backgroundColor else Color.Gray
        }

        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .size(44.dp)
                .background(
                    color = bgColor,
                    shape = iconShape
                )
                .border(
                    width = if (useExtendedColors && enabled) 2.dp else 0.dp,
                    color = if (useExtendedColors) extendedColors.border else Color.Transparent,
                    shape = iconShape
                )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (enabled) Color.White else Color.LightGray,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}