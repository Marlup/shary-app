package com.shary.app.ui.screens.request.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.shary.app.application.request.RequestDraftFieldAddOutcome
import com.shary.app.core.domain.types.enums.PredefinedKey
import com.shary.app.ui.components.SharyModalAction
import com.shary.app.ui.components.SharyModalComplexity
import com.shary.app.ui.components.SharyModalOperationState
import com.shary.app.ui.components.SharyModalScaffold
import com.shary.app.ui.screens.field.components.InputWithSuggestions

@Composable
fun AddRequestDialog(
    onDismiss: () -> Unit,
    onAddRequest: (String, String) -> RequestDraftFieldAddOutcome
) {
    var key by rememberSaveable { mutableStateOf("") }
    var aliasKey by rememberSaveable { mutableStateOf("") }
    var showAdvanced by rememberSaveable { mutableStateOf(false) }
    var showErrors by rememberSaveable { mutableStateOf(false) }
    var operationError by rememberSaveable { mutableStateOf<String?>(null) }
    val aliasFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val predefinedKeys = remember { PredefinedKey.entries.map { it.key } }

    fun submit() {
        showErrors = true
        val normalizedKey = key.trim()
        if (normalizedKey.isBlank()) {
            operationError = "Requested key is required"
            return
        }

        operationError = when (onAddRequest(normalizedKey, aliasKey.trim())) {
            RequestDraftFieldAddOutcome.ADDED -> {
                onDismiss()
                null
            }
            RequestDraftFieldAddOutcome.DUPLICATE ->
                "Requested key '$normalizedKey' is already in this draft"
            RequestDraftFieldAddOutcome.INVALID -> "Requested key is required"
        }
    }

    val operationState = operationError
        ?.let { message -> SharyModalOperationState.Error(message) }
        ?: SharyModalOperationState.Idle

    SharyModalScaffold(
        title = "Add requested field",
        subtitle = "Build the outgoing request draft",
        onDismissRequest = onDismiss,
        complexity = SharyModalComplexity.Compact,
        operationState = operationState,
        isDirty = key.isNotBlank() || aliasKey.isNotBlank(),
        primaryAction = SharyModalAction(
            label = "Add to draft",
            onClick = ::submit,
            enabled = !showErrors || key.isNotBlank()
        )
    ) {
        Text(
            text = "Required",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(8.dp))

        InputWithSuggestions(
            key = key,
            onKeyChange = {
                key = it
                operationError = null
            },
            predefinedKeys = predefinedKeys,
            label = "Key *",
            showError = showErrors,
            supportingText = {
                Text(
                    if (showErrors && key.isBlank()) {
                        "Required"
                    } else {
                        "Choose a known key or enter your own"
                    }
                )
            },
            keyboardOptions = KeyboardOptions(
                imeAction = if (showAdvanced) ImeAction.Next else ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onNext = { aliasFocusRequester.requestFocus() },
                onDone = {
                    keyboardController?.hide()
                    submit()
                }
            )
        )

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showAdvanced = !showAdvanced }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (showAdvanced) {
                    Icons.Filled.ExpandLess
                } else {
                    Icons.Filled.ExpandMore
                },
                contentDescription = null
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "Advanced options",
                style = MaterialTheme.typography.labelLarge
            )
        }

        if (showAdvanced) {
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = aliasKey,
                onValueChange = {
                    aliasKey = it
                    operationError = null
                },
                label = { Text("Alias") },
                supportingText = { Text("Optional display name for this requested field") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                        submit()
                    }
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(aliasFocusRequester)
            )
        }
    }
}
