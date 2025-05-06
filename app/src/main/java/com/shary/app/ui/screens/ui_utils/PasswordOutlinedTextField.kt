package com.shary.app.ui.screens.ui_utils

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

@Composable
fun PasswordOutlinedTextField(
    secret: String,
    isVisible: Boolean,
    onValueChange: (String) -> Unit,
    onClick: () -> Unit
) {

    OutlinedTextField(
        value = secret,
        onValueChange = onValueChange,//{ secret = it },
        label = { Text("Confirm Password") },
        singleLine = true,
        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            val image = if (isVisible)
                Icons.Default.Lock
            else Icons.Default.Lock

            val description = if (isVisible) "Hide password" else "Show password"

            IconButton(onClick = onClick) {
                Icon(imageVector = image, contentDescription = description)
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}