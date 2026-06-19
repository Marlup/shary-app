package com.shary.app.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shary.app.ui.theme.SharyRadius
import com.shary.app.ui.theme.SurfaceLight
import com.shary.app.ui.theme.SurfaceMid
import com.shary.app.ui.theme.Violet200
import com.shary.app.ui.theme.Violet500
import com.shary.app.ui.theme.Violet600
import com.shary.app.ui.theme.Violet900

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SharySearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    activeFilter: String? = null,
    filters: List<String> = emptyList(),
    onFilterChange: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceLight)
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = SurfaceMid, shape = SharyRadius.input)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = com.shary.app.ui.theme.AliasMuted,
                modifier = Modifier.size(16.dp)
            )
            androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
            androidx.compose.foundation.text.BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Violet900),
                singleLine = true,
                decorationBox = { inner ->
                    if (query.isBlank()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyMedium,
                            color = com.shary.app.ui.theme.AliasMuted
                        )
                    }
                    inner()
                }
            )
        }

        if (filters.isNotEmpty() && activeFilter != null && onFilterChange != null) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                filters.forEach { filter ->
                    val isSelected = filter == activeFilter
                    val bg = if (isSelected) Violet600 else SurfaceLight
                    val fg = if (isSelected) Color(0xFFF0ECFF) else Violet500
                    val border = if (isSelected) Violet600 else Violet200
                    Text(
                        text = filter,
                        style = MaterialTheme.typography.labelMedium,
                        color = fg,
                        modifier = Modifier
                            .background(bg, SharyRadius.chip)
                            .border(1.dp, border, SharyRadius.chip)
                            .minimumInteractiveComponentSize()
                            .clickable { onFilterChange(filter) }
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
