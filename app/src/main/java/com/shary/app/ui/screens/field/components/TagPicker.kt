package com.shary.app.ui.screens.field.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
//import androidx.hilt.navigation.compose.hiltViewModel // deprecated location of hiltViewModel
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.shary.app.core.domain.types.enums.Tag
import com.shary.app.core.domain.types.enums.safeColor
import com.shary.app.viewmodels.tag.TagViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagPicker(
    selectedTag: Tag,
    onTagSelected: (Tag) -> Unit,
    tagViewModel: TagViewModel = hiltViewModel()
) {
    val tags by tagViewModel.tags.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<Pair<String, Color>?>(null) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        TextField(
            value = selectedTag.toTagString(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Tag") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            leadingIcon = {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(selectedTag.safeColor())
                )
            }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            tags.forEach { tag ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(tag.safeColor())
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(tag.toTagString())
                        }
                    },
                    onClick = {
                        onTagSelected(tag)
                        expanded = false
                    },
                    trailingIcon = {
                        Row {
                            IconButton(onClick = {
                                showEditDialog = tag.toTagString() to tag.safeColor()
                                expanded = false
                            }) { Icon(Icons.Default.Edit, "Edit") }
                            IconButton(onClick = {
                                tagViewModel.removeTag(tag.toTagString())
                                expanded = false
                            }) { Icon(Icons.Default.Delete, "Delete") }
                        }
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
        AddNewTagDialog(
            initialName = "",
            initialColor = Color.Gray,
            onConfirm = { name, color ->
                Log.d("FieldsScreen()", "tag : ${name}, color : $color")

                tagViewModel.addTag(name, color)
                onTagSelected(Tag.fromString(name, color))
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    showEditDialog?.let { (name, color) ->
        AddNewTagDialog(
            initialName = name,
            initialColor = color,
            onConfirm = { n, c ->
                tagViewModel.updateTag(n, c)
                onTagSelected(Tag.fromString(n, c))
                showEditDialog = null
            },
            onDismiss = { showEditDialog = null }
        )
    }
}
