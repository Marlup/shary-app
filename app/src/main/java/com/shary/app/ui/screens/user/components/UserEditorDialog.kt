package com.shary.app.ui.screens.user.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.shary.app.core.domain.models.UserDomain
import com.shary.app.core.domain.types.enums.Tag
import com.shary.app.ui.components.SharyModalAction
import com.shary.app.ui.components.SharyModalComplexity
import com.shary.app.ui.components.SharyModalOperationState
import com.shary.app.ui.components.SharyModalScaffold
import com.shary.app.ui.screens.field.components.CompactTagSelector
import com.shary.app.utils.Validation
import com.shary.app.viewmodels.tag.TagEditorOperationState
import java.time.Instant

@Composable
fun UserEditorDialog(
    initial: UserDomain,
    title: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (UserDomain) -> Unit,
    isSaving: Boolean = false,
    errorMessage: String? = null,
    availableTags: List<Tag> = emptyList(),
    onCreateTag: ((String, Color) -> Unit)? = null,
    tagEditorState: TagEditorOperationState = TagEditorOperationState.Idle,
    onResetTagEditorState: () -> Unit = {}
) {
    var username by rememberSaveable(initial.email, initial.username) {
        mutableStateOf(initial.username)
    }
    var email by rememberSaveable(initial.email) {
        mutableStateOf(initial.email)
    }
    var selectedTag by rememberSaveable(initial.email) {
        mutableStateOf(initial.tag)
    }
    var showErrors by rememberSaveable(initial.email) { mutableStateOf(false) }
    val emailFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    fun emailValidationMessage(): String =
        Validation.validateEmailSyntax(email.trim())

    fun isValid(): Boolean =
        username.isNotBlank() && emailValidationMessage().isEmpty()

    fun submit() {
        showErrors = true
        if (!isValid() || isSaving) return
        onConfirm(
            initial.copy(
                username = username.trim(),
                email = email.trim(),
                tag = selectedTag,
                dateAdded = if (initial.dateAdded == Instant.EPOCH) {
                    Instant.now()
                } else {
                    initial.dateAdded
                }
            )
        )
    }

    val operationState = when {
        isSaving -> SharyModalOperationState.Saving
        !errorMessage.isNullOrBlank() -> SharyModalOperationState.Error(errorMessage)
        else -> SharyModalOperationState.Idle
    }
    val isDirty = username != initial.username ||
        email != initial.email ||
        selectedTag != initial.tag

    SharyModalScaffold(
        title = title,
        subtitle = "Local contact",
        onDismissRequest = onDismiss,
        complexity = SharyModalComplexity.Compact,
        operationState = operationState,
        isDirty = isDirty,
        primaryAction = SharyModalAction(
            label = confirmLabel,
            onClick = ::submit,
            enabled = !showErrors || isValid()
        )
    ) {
        Text(
            text = "Required",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Alias (local) *") },
            isError = showErrors && username.isBlank(),
            supportingText = {
                if (showErrors && username.isBlank()) {
                    Text("Required")
                } else {
                    Text("Only visible on your device")
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = { emailFocusRequester.requestFocus() }
            ),
            singleLine = true,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth()
        )

        val emailError = if (showErrors) emailValidationMessage() else ""
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email *") },
            isError = emailError.isNotEmpty(),
            supportingText = {
                if (emailError.isNotEmpty()) {
                    Text(emailError)
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide()
                    submit()
                }
            ),
            singleLine = true,
            enabled = !isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(emailFocusRequester)
        )

        if (availableTags.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Group",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            CompactTagSelector(
                selectedTag = selectedTag,
                onTagSelected = { selectedTag = it },
                availableTags = availableTags,
                onCreateTag = onCreateTag,
                tagEditorState = tagEditorState,
                onResetTagEditorState = onResetTagEditorState,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
