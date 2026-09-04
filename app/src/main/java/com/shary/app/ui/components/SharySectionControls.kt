package com.shary.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.shary.app.ui.theme.SurfaceLight
import com.shary.app.ui.theme.Violet200

@Composable
fun SharySectionToolbar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceLight)
            .testTag("shary-section-toolbar")
    ) {
        HorizontalDivider(color = Violet200, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SharySearchBar(
                query = query,
                onQueryChange = onQueryChange,
                placeholder = "Search",
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            actions()
        }
    }
}

@Composable
fun SharyBottomActionShelf(
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceLight)
            .testTag("shary-bottom-action-shelf")
    ) {
        HorizontalDivider(color = Violet200, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
            content = actions
        )
    }
}
