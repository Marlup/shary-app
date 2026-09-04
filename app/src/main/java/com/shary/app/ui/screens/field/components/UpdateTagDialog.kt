package com.shary.app.ui.screens.field.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shary.app.ui.theme.SharyRadius
import com.shary.app.ui.theme.SurfaceLight
import com.shary.app.ui.theme.Violet200
import com.shary.app.ui.theme.Violet500
import com.shary.app.ui.theme.Violet600
import com.shary.app.ui.theme.Violet900
import com.shary.app.core.domain.types.valueobjects.TagNameContract
import com.shary.app.core.domain.types.valueobjects.TagNameValidation
import com.shary.app.core.domain.types.valueobjects.defaultMessage

@Composable
fun UpdateTagDialog(
    currentName: String,
    currentColor: Color,
    onConfirm: (String, Color) -> Unit,
    onDismiss: () -> Unit,
    isSaving: Boolean = false,
    errorMessage: String? = null,
    onDraftChange: () -> Unit = {}
) {
    var name by remember { mutableStateOf(currentName) }
    var selectedColor by remember { mutableStateOf(currentColor) }
    val validation = TagNameContract.validate(name)
    val validationMessage = (validation as? TagNameValidation.Invalid)
        ?.takeIf { name.isNotBlank() }
        ?.reason
        ?.defaultMessage()

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        shape = SharyRadius.dialog,
        containerColor = SurfaceLight,
        tonalElevation = 0.dp,
        title = {
            Text(
                text = "Update tag",
                style = MaterialTheme.typography.headlineLarge,
                color = Violet900
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        onDraftChange()
                    },
                    enabled = !isSaving,
                    label = { Text("Tag name") },
                    singleLine = true,
                    shape = SharyRadius.input,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Violet600,
                        unfocusedBorderColor = Violet200
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                TagColorEditor(
                    color = selectedColor,
                    onColorChanged = {
                        selectedColor = it
                        onDraftChange()
                    },
                    modifier = Modifier.padding(top = 6.dp),
                    enabled = !isSaving
                )

                (errorMessage ?: validationMessage)?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text(text = "Cancel", color = Violet500, style = MaterialTheme.typography.labelLarge)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val validName = (validation as? TagNameValidation.Valid)?.normalizedName
                    if (validName != null) onConfirm(validName, selectedColor)
                },
                enabled = validation is TagNameValidation.Valid && !isSaving
            ) {
                Text(
                    text = if (isSaving) "Saving..." else "Update",
                    color = Violet600,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    )
}
