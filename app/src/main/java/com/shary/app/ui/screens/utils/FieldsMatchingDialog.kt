package com.shary.app.ui.screens.utils

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.shary.app.Field
import com.shary.app.ui.screens.fields.utils.AddFieldDialog

@Composable
fun MatchingDialog(
    storedFields: List<Field>,
    requestKeys: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onAccept: (List<Field>) -> Unit,
    onAddField: (String, String, String) -> Unit,
) {
    var selectedStorageIndex by remember { mutableStateOf<Int?>(null) }
    var selectedRequestIndex by remember { mutableStateOf<Int?>(null) }
    val matches = remember { mutableStateListOf<Triple<Int, Int, Int>>() } // Triple<storageIndex, reqIndex, matchId>
    val storedFieldsSelection = remember { mutableStateListOf<Field>() }
    var openAddDialog by remember { mutableStateOf<Boolean>(false) }
    var freeLabelsFromUnmatched = remember { mutableStateListOf<Int>() }
    var isStorageFirst by remember { mutableStateOf(false) }

    fun closeAddDialog() {
        openAddDialog = false
    }

    fun matchIfPossible() {
        FunctionUtils.matchIfPossible(
            selectedStorageIndex,
            selectedRequestIndex,
            matches,
            storedFieldsSelection,
            storedFields,
            freeLabelsFromUnmatched,
            isStorageFirst,
            onUnselect = {
                selectedStorageIndex = null
                selectedRequestIndex = null
            },
            onIsStorageFirst = {
                isStorageFirst = it
            },
            onMatchCreated = { newMatch ->
                // opcional: log, side effects
            },
            onFreeLabelStored = {
                freeLabelsFromUnmatched.add(it)
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.medium) {
            Column(modifier = Modifier.padding(16.dp)) {
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss)
                    {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (matches.size == requestKeys.size) {
                                onAccept(storedFieldsSelection)
                                onDismiss()
                            }
                        },
                        enabled = matches.size == requestKeys.size
                    ) {
                        Text("Accept")
                    }
                    // Add row button
                    FloatingActionButton(onClick = { openAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Field")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row {
                    // Storage keys column
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        itemsIndexed(storedFields) { index, field ->
                            val match = matches.find { it.first == index }
                            val isSelected = selectedStorageIndex == index
                            SelectableRow(
                                item = field.key,
                                index = index,
                                isSelected = isSelected,
                                onCheckedChange = {
                                    if (matches.size == requestKeys.size && isSelected) {
                                        selectedStorageIndex = null
                                        storedFieldsSelection.remove(field)
                                    } else {
                                        selectedStorageIndex = if (isSelected) null else index
                                        matchIfPossible()
                                    }
                                }
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = field.key, maxLines = 1, style = MaterialTheme.typography.bodyLarge)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        match?.let {
                                            if (it.third > 0)
                                                Text("[${it.third}]", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                    Text(field.value, maxLines = 1, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    // Request keys column
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp)
                    ) {
                        itemsIndexed(requestKeys) { index, (key, value) ->
                            val match = matches.find { it.second == index }
                            val isSelected = selectedRequestIndex == index
                            SelectableRow(
                                item = key,
                                index = index,
                                isSelected = isSelected,
                                onCheckedChange = {
                                    if (matches.size == requestKeys.size && isSelected) {
                                        selectedRequestIndex = null
                                    } else {
                                        selectedRequestIndex = if (isSelected) null else index
                                        matchIfPossible()
                                    }
                                }
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = key, maxLines = 1, style = MaterialTheme.typography.bodyLarge)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        match?.let {
                                            if (it.third > 0)
                                                Text("[${it.third}]", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                    Text(value, maxLines = 1, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
                if (openAddDialog) {
                    AddFieldDialog(
                        onDismiss = { closeAddDialog() },
                        onAddField = { key, keyAlias, value -> onAddField(key, keyAlias, value) }
                    )
                }
            }
        }
    }
}
