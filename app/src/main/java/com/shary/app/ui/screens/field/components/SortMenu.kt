package com.shary.app.ui.screens.field.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.shary.app.core.domain.types.enums.SortByParameter
import com.shary.app.viewmodels.field.FieldViewModel

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
        IconButton(onClick = { expanded = true }) {
            Icon(
                Icons.AutoMirrored.Filled.Sort,
                contentDescription = "Sort"
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            SortByParameter.entries.forEach { option ->
                val isAscending = isAscendingMap[option]!!
                DropdownMenuItem(
                    text = { Text(option.name) },
                    trailingIcon = {
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
