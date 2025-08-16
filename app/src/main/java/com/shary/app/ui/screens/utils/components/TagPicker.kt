package com.shary.app.ui.screens.utils.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shary.app.core.domain.types.enums.UiFieldTag
import com.shary.app.core.domain.types.enums.tagColor


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagPicker(
    selected: String,
    onSelected: (String) -> Unit,
    allowNone: Boolean = true,
    allTags: List<UiFieldTag>
) {

    var expanded by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    val selectedLabel = selected ?: "No tag"
    val chipColor = tagColor(UiFieldTag.fromString(selectedLabel))

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        TextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text("Tag") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            leadingIcon = {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(chipColor)
                )
            }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (allowNone) {
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(tagColor(UiFieldTag.Unknown))
                            )
                            Text("No tag", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    },
                    onClick = {
                        onSelected("")
                        expanded = false
                    }
                )
            }

            allTags.forEach { tag ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(tagColor(tag))
                            )
                            Text(tag.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    },
                    onClick = {
                        onSelected(tag.name)
                        expanded = false
                    }
                )
            }

            HorizontalDivider()

            DropdownMenuItem(
                text = { Text("➕ Create new tag") },
                onClick = {
                    expanded = false
                    showAddDialog = true
                }
            )
        }
    }

    if (showAddDialog) {
        AddCustomTagDialog(
            onAdd = { newTag ->
                onSelected(newTag)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}
