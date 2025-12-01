package com.shary.app.ui.screens.field.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.shary.app.core.domain.types.enums.SortFieldBy

@Composable
fun SortMenu(
    currentSort: SortFieldBy,
    isDescending: Boolean,
    onSortChange: (SortFieldBy, Boolean) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            val options = SortFieldBy.entries
            options.forEach { option ->
                //val selected = option.equals(currentSort)
                DropdownMenuItem(
                    text = { Text(option.name) },
                    trailingIcon = {
                        IconButton(onClick = {
                            //if (selected) onSortChange(option, !isDescending)
                            onSortChange(option, !isDescending)
                        }) {
                            Icon(
                                //imageVector = if (selected && isDescending)
                                imageVector = if (isDescending)
                                    Icons.Default.ArrowDownward
                                else
                                    Icons.Default.ArrowUpward,
                                contentDescription = "Toggle order"
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        onSortChange(option, isDescending)
                    }
                )
            }
        }
    }
}
