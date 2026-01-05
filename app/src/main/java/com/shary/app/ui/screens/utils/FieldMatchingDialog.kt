package com.shary.app.ui.screens.utils

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.types.enums.safeColor
import com.shary.app.ui.screens.field.components.AddFieldDialog


@Composable
fun FieldMatchingDialog(
    storedFields: List<FieldDomain>,
    requestKeys: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onAccept: (List<FieldDomain>) -> Unit,
    onAddField: (FieldDomain) -> Unit,
) {
    val localStoredFields = remember { mutableStateListOf<FieldDomain>() }
    var selectedStorageIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedRequestIndex by rememberSaveable { mutableStateOf<Int?>(null) }

    // Triple<storageIdx, requestIdx, matchId>
    val matches = remember { mutableStateListOf<Triple<Int, Int, Int>>() }
    val freeLabelsFromUnmatched = remember { mutableStateListOf<Int>() }

    var isStorageFirst by rememberSaveable { mutableStateOf(false) }
    var openAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(storedFields) {
        localStoredFields.clear()
        localStoredFields.addAll(storedFields)
    }

    fun clearPendingSelections() {
        selectedStorageIndex = null
        selectedRequestIndex = null
    }

    fun matchIfPossible() {
        FunctionUtils.matchIfPossible(
            storageIdx = selectedStorageIndex,
            requestIdx = selectedRequestIndex,
            matches = matches,
            freeLabelsFromUnmatched = freeLabelsFromUnmatched,
            isStorageFirst = isStorageFirst,
            onUnselect = { clearPendingSelections() },
            onIsStorageFirst = { isStorageFirst = it },
            onFreeLabelStored = { freeLabelsFromUnmatched.add(it) }
        )
    }

    // Para aceptar devolvemos una lista derivada de matches (evita duplicados por re-matches)
    fun buildAcceptedSelection(): List<FieldDomain> =
        matches.sortedBy { it.second }
            .mapNotNull { (sIdx, _, _) -> localStoredFields.getOrNull(sIdx) }

    val isFullyMatched = matches.size == requestKeys.size

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, tonalElevation = 3.dp) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .heightIn(min = 200.dp, max = 560.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (isFullyMatched) {
                                onAccept(buildAcceptedSelection())
                                onDismiss()
                            }
                        },
                        enabled = isFullyMatched
                    ) { Text("Accept") }
                    Spacer(Modifier.width(12.dp))
                    SmallFloatingActionButton(onClick = { openAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Field")
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    // Columna izquierda: almacen (storedFields)
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                            .heightIn(max = 420.dp)
                    ) {
                        itemsIndexed(
                            items = localStoredFields,
                            key = { index, field -> "${field.key}#$index" }
                        ) { index, field ->
                            val isSelected = selectedStorageIndex == index
                            val isMatched = matches.any { it.first == index }

                            val backgroundColor = when {
                                isSelected -> field.tag.safeColor() // highlight with tag color
                                isMatched -> MaterialTheme.colorScheme.secondaryContainer   // matched highlight
                                index % 2 == 0 -> MaterialTheme.colorScheme.surfaceVariant    // alternate background
                                else -> MaterialTheme.colorScheme.surface
                            }

                            SelectableRow(
                                item = field.key,
                                index = index,
                                backgroundColorProvider = { backgroundColor },
                                onToggle = {
                                    selectedStorageIndex = if (isSelected) null else index
                                    matchIfPossible()
                                }
                            ) {
                                Column(Modifier.fillMaxWidth()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            field.key,
                                            maxLines = 1,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = backgroundColor
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        matches.firstOrNull { it.first == index }?.let { triple ->
                                            if (triple.third > 0) {
                                                Text("[${triple.third}]", style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                        /*
                                        if (isMatched) {
                                            Spacer(Modifier.width(8.dp))
                                            AssistChip(onClick = {}, label = { Text("Matched") })
                                        }
                                         */
                                    }
                                    Text(field.value, maxLines = 1, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    // Columna derecha: requestKeys
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp)
                            .heightIn(max = 420.dp)
                    ) {
                        itemsIndexed(
                            items = requestKeys,
                            key = { index, pair -> "${pair.first}#$index" }
                        ) { index, (key, value) ->
                            val isSelected = selectedRequestIndex == index
                            val match = matches.firstOrNull { it.second == index }
                            val isMatched = match != null

                            val rowBackgroundColor = when {
                                isSelected -> Color.LightGray // ← selection color
                                index % 2 == 0 -> MaterialTheme.colorScheme.surface                     // alternate / tag color
                                else -> MaterialTheme.colorScheme.secondaryContainer
                            }

                            SelectableRow(
                                item = key,
                                index = index,
                                backgroundColorProvider = { rowBackgroundColor },
                                onToggle = {
                                    selectedRequestIndex = if (isSelected) null else index
                                    matchIfPossible()
                                }
                            ) {
                                Column(Modifier.fillMaxWidth()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            key,
                                            maxLines = 1,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = rowBackgroundColor
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        match?.let {
                                            if (it.third > 0) {
                                                Text("[${it.third}]", style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                        /*
                                        if (isMatched) {
                                            Spacer(Modifier.width(8.dp))
                                            AssistChip(onClick = {}, label = { Text("Matched") })
                                        }
                                         */
                                    }
                                    Text(value, maxLines = 1, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                if (openAddDialog) {
                    AddFieldDialog(
                        onDismiss = { openAddDialog = false },
                        onAddField = { newField ->
                            localStoredFields.add(newField)
                            onAddField(newField)
                            openAddDialog = false
                        },
                    )
                }
            }
        }
    }
}
