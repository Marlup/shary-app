package com.shary.app.ui.screens.field.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.shary.app.core.domain.types.enums.SortByParameter
import com.shary.app.ui.screens.utils.LongPressHint

@Composable
fun SortMenu(
    currentSort: SortByParameter,
    isAscendingMap: Map<SortByParameter, Boolean>,
    onSortChange: (SortByParameter, Boolean) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    // ======== Sort Fields Parameters ========

    // 🔹 React to sort changes
    LaunchedEffect(currentSort) {
        if (expanded) {
            listState.scrollToItem(0)
        }
    }

    Box {
        LongPressHint("Open sorting options") {
            IconButton(onClick = { expanded = true }) {
                Icon(
                    imageVector = Icons.Default.SwapVert,
                    contentDescription = "Sort fields"
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            SortByParameter.entries.forEach { option ->
                val isAscending = isAscendingMap[option]!!
                DropdownMenuItem(
                    text = { Text(option.label) },
                    leadingIcon = if (option == currentSort) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null
                            )
                        }
                    } else {
                        null
                    },
                    trailingIcon = {
                        LongPressHint("Toggle ascending or descending order") {
                            IconButton(onClick = { onSortChange(option, !isAscending) }
                            ) {
                                Icon(
                                    imageVector = if (isAscending)
                                        Icons.Default.ArrowUpward
                                    else
                                        Icons.Default.ArrowDownward,
                                    contentDescription = "Toggle order"
                                )
                            }
                        }
                    },
                    onClick = {
                        expanded = false
                        onSortChange(option, isAscending)
                    }
                )
            }
        }
    }
}
