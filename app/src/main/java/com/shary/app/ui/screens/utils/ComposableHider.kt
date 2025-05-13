package com.shary.app.ui.screens.utils

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp

@Composable
fun SearchToggleWithRowSearcher(
    showSearcher: Boolean,
    onValueChange: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .padding(8.dp)
            .border(2.dp, Color.Black, shape = RectangleShape)
        //verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { onValueChange(showSearcher) }
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = if (showSearcher) "Hide searcher" else "Show searcher"
            )
        }

        AnimatedVisibility(
            visible = showSearcher,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            content()
        }
    }
}
