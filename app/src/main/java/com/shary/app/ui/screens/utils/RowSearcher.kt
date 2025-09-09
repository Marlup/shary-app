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
fun <A> RowSearcher(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    currentAttribute: A,
    onAttributeChange: (A) -> Unit,
    availableAttributes: List<A>,
    resolveOptionText: (A) -> String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val optionText = resolveOptionText(currentAttribute)

        TextField(
            value = searchText,
            onValueChange = onSearchTextChange,
            label = {
                Text(
                    text = "Search by ${optionText.lowercase()}",
                    maxLines = 1
                )
            },
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .padding(vertical = 4.dp, horizontal = 4.dp),
            singleLine = true,
            maxLines = 1
        )

        FilterBox(
            optionText,
            onClick = {
                // cycle through attributes or toggle between 2
                val currentIndex = availableAttributes.indexOf(currentAttribute)
                val next = availableAttributes[(currentIndex + 1) % availableAttributes.size]
                onAttributeChange(next)
            }
        )
    }
}
