package com.shary.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun PrimaryTabOverflowMenu(
    onOpenSettings: () -> Unit,
    onChangePassword: () -> Unit,
    onLogout: () -> Unit,
    extraItems: (@Composable (dismissMenu: () -> Unit) -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val dismissMenu = { expanded = false }

    Box {
        SharyIconButton(
            icon = Icons.Default.MoreVert,
            contentDescription = "More options",
            onClick = { expanded = true }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = dismissMenu
        ) {
            extraItems?.invoke(dismissMenu)
            if (extraItems != null) {
                HorizontalDivider()
            }
            DropdownMenuItem(
                text = { Text("Settings") },
                onClick = {
                    dismissMenu()
                    onOpenSettings()
                },
                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("Change password") },
                onClick = {
                    dismissMenu()
                    onChangePassword()
                },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("Logout") },
                onClick = {
                    dismissMenu()
                    onLogout()
                },
                leadingIcon = {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                }
            )
        }
    }
}
