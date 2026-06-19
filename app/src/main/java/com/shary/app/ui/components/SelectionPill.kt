package com.shary.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shary.app.ui.theme.SelectionBg
import com.shary.app.ui.theme.SelectionText
import com.shary.app.ui.theme.SharyRadius
import com.shary.app.ui.theme.Violet500

@Composable
fun SelectionPill(
    selectedCount: Int,
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(SelectionBg, SharyRadius.chip)
            .padding(start = 10.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "$selectedCount selected",
            style = MaterialTheme.typography.bodySmall,
            color = SelectionText
        )
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Clear selection",
            tint = Violet500,
            modifier = Modifier
                .size(16.dp)
                .minimumInteractiveComponentSize()
                .clickable(onClick = onClearSelection)
        )
    }
}
