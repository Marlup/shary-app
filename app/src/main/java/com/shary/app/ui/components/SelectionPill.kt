package com.shary.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ClearSelectionButton(
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    SharyIconButton(
        icon = Icons.Default.Deselect,
        contentDescription = "Deselect all",
        onClick = onClearSelection,
        modifier = modifier,
        buttonSize = 52.dp
    )
}
