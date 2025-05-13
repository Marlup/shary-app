package com.shary.app.ui.screens.utils

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RowSearcher(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    searchByFirstColumn: Boolean,
    onSearchByChange: (Boolean) -> Unit,
    optionsSearchBy: Pair<String, String>
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        val optionText = if (searchByFirstColumn)
            optionsSearchBy.first
        else
            optionsSearchBy.second

        // 🔍 Search and Toggle Filter
        TextField(
            value = searchText,
            onValueChange = onSearchTextChange,
            label = { Text("Search by ${optionText.lowercase()}", maxLines=1) },
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .padding(vertical = 4.dp, horizontal = 4.dp),
            singleLine = true,
            maxLines = 1
        )

        FilterBox(
            optionText,
            isSelected = searchByFirstColumn,
            onClick = { onSearchByChange(!searchByFirstColumn) } // { searchByFirstColumn = true },
        )
    }
}