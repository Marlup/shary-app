package com.shary.app.ui.screens.field.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.shary.app.core.domain.types.enums.Tag
import com.shary.app.core.domain.types.enums.key
import com.shary.app.core.domain.types.enums.safeColor
import com.shary.app.ui.components.SharySoftButton
import com.shary.app.ui.screens.utils.LongPressHint
import com.shary.app.utils.log.AppLogger
import com.shary.app.viewmodels.field.FieldViewModel
import com.shary.app.viewmodels.tag.TagViewModel
import com.shary.app.viewmodels.tag.TagEditorOperation
import com.shary.app.viewmodels.tag.TagEditorOperationState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagPicker(
    selectedTag: Tag,
    onTagSelected: (Tag) -> Unit,
    tagViewModel: TagViewModel = hiltViewModel(),
    fieldViewModel: FieldViewModel = hiltViewModel()
) {
    val tags by tagViewModel.tags.collectAsState()
    val editorOperationState by tagViewModel.editorOperationState.collectAsState()
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
                tagViewModel.resetEditorOperationState()
            }
            TagEditorOperation.UPDATE -> if (showUpdateDialog && tagToUpdate != null) {
                if (selectedTag.key == saved.identity) {
                    onTagSelected(saved.tag)
                }
                showUpdateDialog = false
                tagToUpdate = null
                tagViewModel.resetEditorOperationState()
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
            label = { Text("Tag") },
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
                            LongPressHint("Edit this tag") {
                                IconButton(onClick = {
                                    // Check if tag is in use before allowing edit
                                    if (fieldViewModel.isTagInUse(tag)) {
                                        errorMessage = "Cannot update tag '${tag.toTagString()}' - it is currently in use by fields"
                                    } else {
                                        tagViewModel.resetEditorOperationState()
                                        tagToUpdate = tag
                                        showUpdateDialog = true
                                    }
                                }) { Icon(Icons.Default.Edit, "Edit") }
                            }
                            LongPressHint("Delete this tag") {
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
                    }
                )
            }

            HorizontalDivider()

            DropdownMenuItem(
                text = { Text("➕ Create new tag") },
                onClick = {
                    expanded = false
                    tagViewModel.resetEditorOperationState()
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
                AppLogger.debug("TagPicker", "event=create_tag")
                tagViewModel.submitAddTag(name, color)
            },
            onDismiss = {
                showAddDialog = false
                tagViewModel.resetEditorOperationState()
            },
            isSaving = editorOperationState is TagEditorOperationState.Saving &&
                (editorOperationState as TagEditorOperationState.Saving).operation == TagEditorOperation.ADD,
            errorMessage = (editorOperationState as? TagEditorOperationState.Failed)
                ?.takeIf { it.operation == TagEditorOperation.ADD }
                ?.message,
            onDraftChange = tagViewModel::resetEditorOperationState
        )
    }

    if (showUpdateDialog && tagToUpdate != null) {
        UpdateTagDialog(
            currentName = tagToUpdate!!.toTagString(),
            currentColor = tagToUpdate!!.safeColor(),
            onConfirm = { name, color ->
                AppLogger.debug("TagPicker", "event=update_tag")
                tagViewModel.submitUpdateTag(
                    originalName = checkNotNull(tagToUpdate).toTagString(),
                    name = name,
                    color = color
                )
            },
            onDismiss = {
                showUpdateDialog = false
                tagToUpdate = null
                tagViewModel.resetEditorOperationState()
            },
            isSaving = editorOperationState is TagEditorOperationState.Saving &&
                (editorOperationState as TagEditorOperationState.Saving).operation == TagEditorOperation.UPDATE,
            errorMessage = (editorOperationState as? TagEditorOperationState.Failed)
                ?.takeIf { it.operation == TagEditorOperation.UPDATE }
                ?.message,
            onDraftChange = tagViewModel::resetEditorOperationState
        )
    }

    // Error message dialog
    errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("Cannot modify tag") },
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

@Composable
fun CompactTagSelector(
    selectedTag: Tag,
    onTagSelected: (Tag) -> Unit,
    modifier: Modifier = Modifier,
    availableTags: List<Tag>? = null,
    tagViewModel: TagViewModel? = null,
    onCreateTag: ((String, Color) -> Unit)? = null,
    tagEditorState: TagEditorOperationState = TagEditorOperationState.Idle,
    onResetTagEditorState: () -> Unit = {}
) {
    val resolvedTagViewModel = if (availableTags == null) {
        tagViewModel ?: hiltViewModel<TagViewModel>()
    } else {
        tagViewModel
    }
    val tags = availableTags ?: checkNotNull(resolvedTagViewModel).tags.collectAsState().value
    val resolvedEditorState = resolvedTagViewModel
        ?.editorOperationState
        ?.collectAsState()
        ?.value
        ?: tagEditorState
    val createTag: ((String, Color) -> Unit)? = onCreateTag
        ?: resolvedTagViewModel?.let { viewModel ->
            { name: String, color: Color -> viewModel.submitAddTag(name, color) }
        }
    val resetEditorState = resolvedTagViewModel?.let { viewModel ->
        { viewModel.resetEditorOperationState() }
    } ?: onResetTagEditorState

    CompactTagSelectorContent(
        selectedTag = selectedTag,
        onTagSelected = onTagSelected,
        availableTags = tags,
        onCreateTag = createTag,
        tagEditorState = resolvedEditorState,
        onResetTagEditorState = resetEditorState,
        modifier = modifier
    )
}

@Composable
internal fun CompactTagSelectorContent(
    selectedTag: Tag,
    onTagSelected: (Tag) -> Unit,
    availableTags: List<Tag>,
    onCreateTag: ((String, Color) -> Unit)?,
    tagEditorState: TagEditorOperationState,
    onResetTagEditorState: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(tagEditorState) {
        val saved = tagEditorState as? TagEditorOperationState.Saved
            ?: return@LaunchedEffect
        if (saved.operation == TagEditorOperation.ADD && showAddDialog) {
            onTagSelected(saved.tag)
            showAddDialog = false
            onResetTagEditorState()
        }
    }

    Box(modifier = modifier) {
        SharySoftButton(
            text = selectedTag.toTagString(),
            icon = Icons.Default.LocalOffer,
            modifier = Modifier.fillMaxWidth(),
            onClick = { expanded = true }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            availableTags.forEach { tag ->
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
                    }
                )
            }
            if (onCreateTag != null) {
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Create new tag") },
                    leadingIcon = { Icon(Icons.Default.LocalOffer, contentDescription = null) },
                    onClick = {
                        expanded = false
                        onResetTagEditorState()
                        showAddDialog = true
                    }
                )
            }
        }
    }

    if (showAddDialog && onCreateTag != null) {
        AddNewTagDialog(
            initialName = "",
            initialColor = Color.Gray,
            onConfirm = { name, color ->
                onCreateTag(name, color)
            },
            onDismiss = {
                showAddDialog = false
                onResetTagEditorState()
            },
            isSaving = tagEditorState is TagEditorOperationState.Saving &&
                tagEditorState.operation == TagEditorOperation.ADD,
            errorMessage = (tagEditorState as? TagEditorOperationState.Failed)
                ?.takeIf { it.operation == TagEditorOperation.ADD }
                ?.message,
            onDraftChange = onResetTagEditorState
        )
    }
}
