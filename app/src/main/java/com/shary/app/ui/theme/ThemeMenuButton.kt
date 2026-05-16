package com.shary.app.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.shary.app.core.domain.types.enums.AppTheme
import com.shary.app.ui.screens.utils.LongPressHint

@Composable
fun ThemeMenuButton(
    enabled: Boolean = true,
    onThemeChosen: (theme: AppTheme) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    // Theme menu button
    Box {
        LongPressHint(if (enabled) "Open theme picker" else "Theme selector pending rework") {
            IconButton(
                onClick = { expanded = true },
                enabled = enabled,
                colors = IconButtonDefaults.iconButtonColors()
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = "Choose Theme"
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            AppTheme.entries.forEach { theme ->
                DropdownMenuItem(
                    text = { Text(theme.name) },
                    onClick = {
                        expanded = false
                        onThemeChosen(theme)
                    }
                )
            }
        }
    }
}
