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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.shary.app.core.domain.types.enums.Tag
import com.shary.app.core.domain.types.enums.safeColor
import com.shary.app.viewmodels.field.FieldViewModel
import com.shary.app.viewmodels.tag.TagViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagPicker(
    selectedTag: Tag,
    onTagSelected: (Tag) -> Unit,
    tagViewModel: TagViewModel = hiltViewModel(),
    fieldViewModel: FieldViewModel = hiltViewModel()
) {
    val tags by tagViewModel.tags.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var tagToUpdate by remember { mutableStateOf<Tag?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
                                // Check if tag is in use before allowing edit
                                if (fieldViewModel.isTagInUse(tag)) {
                                    errorMessage = "Cannot update tag '${tag.toTagString()}' - it is currently in use by fields"
                                } else {
                                    tagToUpdate = tag
                                    showUpdateDialog = true
                                }
                            }) { Icon(Icons.Default.Edit, "Edit") }
                            IconButton(onClick = {
                                // Check if tag is in use before allowing delete
                                if (fieldViewModel.isTagInUse(tag)) {
                                    errorMessage = "Cannot delete tag '${tag.toTagString()}' - it is currently in use by fields"
                                } else {
                                    tagViewModel.removeTag(tag.toTagString())
                                }
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

                showAddDialog = false
                tagViewModel.addTag(name, color)
                onTagSelected(Tag.fromString(name, color))
            },
            onDismiss = { showAddDialog = false }
        )
    }

    if (showUpdateDialog && tagToUpdate != null) {
        UpdateTagDialog(
            currentName = tagToUpdate!!.toTagString(),
            currentColor = tagToUpdate!!.safeColor(),
            onConfirm = { name, color ->
                Log.d("TagPicker", "Updating tag: $name with color: $color")
                showUpdateDialog = false
                tagViewModel.updateTag(name, color)
                // Update the selected tag if it was the one being edited
                if (selectedTag == tagToUpdate) {
                    onTagSelected(Tag.fromString(name, color))
                }
                tagToUpdate = null
            },
            onDismiss = {
                showUpdateDialog = false
                tagToUpdate = null
            }
        )
    }

    // Error message dialog
    errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("Cannot modify tag") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) {
                    Text("OK")
                }
            }
        )
    }
}
