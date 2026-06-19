package com.shary.app.ui.screens.field.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.shary.app.ui.theme.SharyRadius
import com.shary.app.ui.theme.SurfaceLight
import com.shary.app.ui.theme.Violet200
import com.shary.app.ui.theme.Violet500
import com.shary.app.ui.theme.Violet600
import com.shary.app.ui.theme.Violet900

@Composable
fun ChangePasswordDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onAccept: (oldPassword: String, newPassword: String, repeatNewPassword: String) -> Unit
) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var repeatNewPassword by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = {
            if (!isLoading) onDismiss()
        },
        shape = SharyRadius.dialog,
        containerColor = SurfaceLight,
        tonalElevation = 0.dp,
        title = {
            Text(
                text = "Change password",
                style = MaterialTheme.typography.headlineLarge,
                color = Violet900
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PasswordInputField(
                    label = "Old password",
                    value = oldPassword,
                    enabled = !isLoading,
                    onValueChange = { oldPassword = it }
                )
                PasswordInputField(
                    label = "New password",
                    value = newPassword,
                    enabled = !isLoading,
                    onValueChange = { newPassword = it }
                )
                PasswordInputField(
                    label = "Repeat new password",
                    value = repeatNewPassword,
                    enabled = !isLoading,
                    onValueChange = { repeatNewPassword = it }
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isLoading,
                onClick = {
                    onAccept(oldPassword, newPassword, repeatNewPassword)
                }
            ) {
                Text(
                    text = if (isLoading) "Processing..." else "Accept",
                    style = MaterialTheme.typography.labelLarge,
                    color = Violet600
                )
            }
        },
        dismissButton = {
            TextButton(
                enabled = !isLoading,
                onClick = onDismiss
            ) {
                Text(
                    text = "Cancel",
                    style = MaterialTheme.typography.labelLarge,
                    color = Violet500
                )
            }
        }
    )
}

@Composable
private fun PasswordInputField(
    label: String,
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = value,
        enabled = enabled,
        onValueChange = onValueChange,
        label = { Text(label) },
        shape = SharyRadius.input,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Violet600,
            unfocusedBorderColor = Violet200
        ),
        visualTransformation = if (isVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        trailingIcon = {
            IconButton(
                enabled = enabled,
                onClick = { isVisible = !isVisible }
            ) {
                Icon(
                    imageVector = if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (isVisible) "Hide password" else "Show password"
                )
            }
        }
    )
}
