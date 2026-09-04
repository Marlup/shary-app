package com.shary.app.ui.screens.user.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.shary.app.core.domain.types.enums.Tag
import com.shary.app.core.domain.types.enums.key
import com.shary.app.core.domain.types.enums.safeColor
import com.shary.app.ui.screens.field.components.AddNewTagDialog
import com.shary.app.ui.screens.field.components.UpdateTagDialog
import com.shary.app.ui.screens.utils.LongPressHint
import com.shary.app.viewmodels.tag.TagEditorOperation
import com.shary.app.viewmodels.tag.TagEditorOperationState
import com.shary.app.viewmodels.user.ContactTagViewModel
import com.shary.app.viewmodels.user.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactTagPicker(
    selectedTag: Tag,
    onTagSelected: (Tag) -> Unit,
    contactTagViewModel: ContactTagViewModel = hiltViewModel(),
    userViewModel: UserViewModel = hiltViewModel()
) {
    val tags by contactTagViewModel.tags.collectAsState()
    val editorOperationState by contactTagViewModel.editorOperationState.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var tagToUpdate by remember { mutableStateOf<Tag?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(editorOperationState) {
        val saved = editorOperationState as? TagEditorOperationState.Saved
            ?: return@LaunchedEffect
        when (saved.operation) {
            TagEditorOperation.ADD -> if (showAddDialog) {
                onTagSelected(saved.tag)
                showAddDialog = false
                contactTagViewModel.resetEditorOperationState()
            }
            TagEditorOperation.UPDATE -> if (showUpdateDialog && tagToUpdate != null) {
                if (selectedTag.key == saved.identity) {
                    onTagSelected(saved.tag)
                }
                showUpdateDialog = false
                tagToUpdate = null
                contactTagViewModel.resetEditorOperationState()
            }
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        TextField(
            value = selectedTag.toTagString(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Group") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor(
                    type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                    enabled = true
                )
                .fillMaxWidth(),
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
                            LongPressHint("Edit this group") {
                                IconButton(onClick = {
                                    if (userViewModel.isTagInUse(tag)) {
                                        errorMessage =
                                            "Cannot update group '${tag.toTagString()}' - it is currently in use by contacts"
                                    } else {
                                        contactTagViewModel.resetEditorOperationState()
                                        tagToUpdate = tag
                                        showUpdateDialog = true
                                    }
                                }) { Icon(Icons.Default.Edit, "Edit") }
                            }
                            LongPressHint("Delete this group") {
                                IconButton(onClick = {
                                    if (userViewModel.isTagInUse(tag)) {
                                        errorMessage =
                                            "Cannot delete group '${tag.toTagString()}' - it is currently in use by contacts"
                                    } else {
                                        contactTagViewModel.removeTag(tag.toTagString())
                                    }
                                }) { Icon(Icons.Default.Delete, "Delete") }
                            }
                        }
                    }
                )
            }

            HorizontalDivider()

            DropdownMenuItem(
                text = { Text("Create new group") },
                onClick = {
                    expanded = false
                    contactTagViewModel.resetEditorOperationState()
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
                contactTagViewModel.submitAddTag(name, color)
            },
            onDismiss = {
                showAddDialog = false
                contactTagViewModel.resetEditorOperationState()
            },
            isSaving = editorOperationState is TagEditorOperationState.Saving &&
                (editorOperationState as TagEditorOperationState.Saving).operation == TagEditorOperation.ADD,
            errorMessage = (editorOperationState as? TagEditorOperationState.Failed)
                ?.takeIf { it.operation == TagEditorOperation.ADD }
                ?.message,
            onDraftChange = contactTagViewModel::resetEditorOperationState
        )
    }

    if (showUpdateDialog && tagToUpdate != null) {
        UpdateTagDialog(
            currentName = tagToUpdate!!.toTagString(),
            currentColor = tagToUpdate!!.safeColor(),
            onConfirm = { name, color ->
                contactTagViewModel.submitUpdateTag(
                    originalName = checkNotNull(tagToUpdate).toTagString(),
                    name = name,
                    color = color
                )
            },
            onDismiss = {
                showUpdateDialog = false
                tagToUpdate = null
                contactTagViewModel.resetEditorOperationState()
            },
            isSaving = editorOperationState is TagEditorOperationState.Saving &&
                (editorOperationState as TagEditorOperationState.Saving).operation == TagEditorOperation.UPDATE,
            errorMessage = (editorOperationState as? TagEditorOperationState.Failed)
                ?.takeIf { it.operation == TagEditorOperation.UPDATE }
                ?.message,
            onDraftChange = contactTagViewModel::resetEditorOperationState
        )
    }

    errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("Cannot modify group") },
            text = { Text(message) },
            confirmButton = {
                LongPressHint("Dismiss this message") {
                    TextButton(onClick = { errorMessage = null }) {
                        Text("OK")
                    }
                }
            }
        )
    }
}
