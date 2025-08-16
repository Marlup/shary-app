package com.shary.app.ui.screens.field.utils.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.shary.app.ui.screens.home.utils.SendOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendFieldsDialog(
    selectedOption: SendOption?,
    onDismiss: () -> Unit,
    onOptionSelected: (SendOption) -> Unit,
    onSend: () -> Unit,
    onLeave: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)

    DisposableEffect(lifecycleOwner.value) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                onLeave()
            }
        }

        val lifecycle = lifecycleOwner.value.lifecycle
        lifecycle.addObserver(observer)

        onDispose {
            lifecycle.removeObserver(observer)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 8.dp
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Send Fields", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))

                // Deployable Menu
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    val scope = this // this is ExposedDropdownMenuBoxScope
                    TextField(
                        value = selectedOption?.label ?: "Select a service",
                        onValueChange = {}, // Read-only, no necesita onChange
                        readOnly = true,
                        label = { Text("Send via") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .menuAnchor() // Esto solo funciona dentro del scope correcto
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        SendOption.all.forEach { option ->
                            if (option != null) {
                                DropdownMenuItem(
                                    text = { option.label },
                                    onClick = {
                                        onOptionSelected(option)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(onClick = onSend) { Text("Accept") }
                }
            }
        }
    }
}
