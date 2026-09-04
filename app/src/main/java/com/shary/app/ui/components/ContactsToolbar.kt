package com.shary.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.shary.app.ui.theme.SharyRadius
import com.shary.app.ui.theme.SurfaceLight
import com.shary.app.ui.theme.TextSubtle
import com.shary.app.ui.theme.Violet200
import com.shary.app.ui.theme.Violet600

/**
 * Contacts-only controls. Sort remains fixed while only the direct group tag
 * filters scroll horizontally.
 */
@Composable
fun ContactsToolbar(
    query: String,
    onQueryChange: (String) -> Unit,
    filters: List<String>,
    activeFilter: String,
    onFilterChange: (String) -> Unit,
    sortAction: @Composable () -> Unit,
    overflowAction: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceLight)
            .testTag("contacts-toolbar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 10.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SharySearchBar(
                query = query,
                onQueryChange = onQueryChange,
                placeholder = "Search contacts",
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            overflowAction()
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.testTag("contacts-static-sort")
            ) {
                sortAction()
            }
            Spacer(Modifier.width(8.dp))
            LazyRow(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("contacts-tag-scroller"),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(filters, key = { it }) { filter ->
                    ContactsFilterChip(
                        label = filter,
                        selected = filter == activeFilter,
                        onClick = { onFilterChange(filter) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactsFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val background = if (selected) Violet600 else Color.White
    val foreground = if (selected) Color.White else TextSubtle
    val border = if (selected) Violet600 else Violet200

    Row(
        modifier = Modifier
            .height(48.dp)
            .semantics {
                this.selected = selected
            }
            .clickable(role = Role.Tab, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = foreground,
            modifier = Modifier
                .background(background, SharyRadius.chip)
                .border(1.dp, border, SharyRadius.chip)
                .padding(horizontal = 13.dp, vertical = 6.dp)
        )
    }
}
