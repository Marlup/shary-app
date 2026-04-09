package com.shary.app.ui.screens.request.utils

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import com.shary.app.core.domain.types.enums.PredefinedKey
import com.shary.app.ui.screens.field.components.InputWithSuggestions
import com.shary.app.ui.screens.utils.LongPressHint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRequestDialog(
    onDismiss: () -> Unit,
    onAddRequest: (String, String) -> Unit
) {
    var key by remember { mutableStateOf("") }
    var aliasKey by remember { mutableStateOf("") }
    var showAdvanced by remember { mutableStateOf(false) }
    var showErrors by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val sheetMaxHeight = (configuration.screenHeightDp * 0.7f).dp
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val predefinedKeys = PredefinedKey.entries.map { it.key }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = sheetMaxHeight)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Add New Request",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Required",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            InputWithSuggestions(
                key = key,
                onKeyChange = { key = it },
                predefinedKeys = predefinedKeys,
                label = "Key *",
                showError = showErrors
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAdvanced = !showAdvanced }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (showAdvanced) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "Advanced options",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            if (showAdvanced) {
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = aliasKey,
                    onValueChange = { aliasKey = it },
                    label = { Text("Alias") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                LongPressHint("Close without adding a request") {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
                LongPressHint("Add this new request") {
                    FilledTonalButton(
                        onClick = {
                            showErrors = true
                            if (key.isBlank()) return@FilledTonalButton
                            onAddRequest(key, aliasKey)
                        }
                    ) {
                        Text("Add")
                    }
                }
            }
        }
    }
}

