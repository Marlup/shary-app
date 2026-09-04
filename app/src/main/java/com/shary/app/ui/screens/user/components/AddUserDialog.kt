package com.shary.app.ui.screens.user.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.shary.app.core.domain.models.UserDomain
import com.shary.app.core.domain.types.enums.Tag
import com.shary.app.viewmodels.tag.TagEditorOperationState
import java.time.Instant

@Composable
fun AddUserDialog(
    onDismiss: () -> Unit,
    onAddUser: (UserDomain) -> Unit,
    isSaving: Boolean = false,
    errorMessage: String? = null,
    availableTags: List<Tag> = emptyList(),
    onCreateTag: ((String, Color) -> Unit)? = null,
    tagEditorState: TagEditorOperationState = TagEditorOperationState.Idle,
    onResetTagEditorState: () -> Unit = {}
) {
    val initial = UserDomain(
        username = "",
        email = "",
        dateAdded = Instant.EPOCH // will be set on confirm
    )

    UserEditorDialog(
        initial = initial,
        title = "Add Contact",
        confirmLabel = "Add",
        onDismiss = onDismiss,
        onConfirm = onAddUser,
        isSaving = isSaving,
        errorMessage = errorMessage,
        availableTags = availableTags,
        onCreateTag = onCreateTag,
        tagEditorState = tagEditorState,
        onResetTagEditorState = onResetTagEditorState
    )
}
