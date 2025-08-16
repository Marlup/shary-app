package com.shary.app.ui.screens.field

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.types.enums.AddFlow
import com.shary.app.core.domain.types.enums.tagColor
import com.shary.app.ui.screens.field.utils.dialogs.AddCopyFieldDialog
import com.shary.app.ui.screens.field.utils.dialogs.AddFieldDialog
import com.shary.app.ui.screens.utils.FieldItemRow
import com.shary.app.ui.screens.utils.GoBackButton
import com.shary.app.ui.screens.utils.RowSearcher
import com.shary.app.ui.screens.utils.SelectableRow
import com.shary.app.utils.DateUtils
import com.shary.app.viewmodels.field.FieldEvent
import com.shary.app.viewmodels.field.FieldViewModel
import com.shary.app.viewmodels.tags.TagViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FieldsScreen(navController: NavHostController) {
    val fieldViewModel: FieldViewModel = hiltViewModel()
    val tagViewModel: TagViewModel = hiltViewModel()

    // ---- Table and DB rows (Domain) ----
    val fieldList by fieldViewModel.fields.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // ---- Add dialogs ----
    var openAddDialog by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var lastSubmittedKey by remember { mutableStateOf<String?>(null) }
    var lastFlow by remember { mutableStateOf(AddFlow.NONE) }

    // ---- Add Copy dialog ----
    var openAddFieldCopyDialog by remember { mutableStateOf(false) }
    var targetAddFieldCopy by remember { mutableStateOf<FieldDomain?>(null) }

    // ---- Editing/Updating field (Domain) ----
    var editingField by remember { mutableStateOf<FieldDomain?>(null) }
    var editedValue by remember { mutableStateOf("") }
    var editedAlias by remember { mutableStateOf("") }

    // ---- Checked rows (Domain) ----
    val selectedFields by fieldViewModel.selectedFields.collectAsState()

    // ---- Available tags ----
    val availableTags by tagViewModel.uiTags.collectAsState()

    // ---- Search Fields ----
    var searchText by remember { mutableStateOf("") }
    var searchByKey by remember { mutableStateOf(true) }

    // Safe filtering (keyAlias is nullable in domain)
    val filteredFields = remember(fieldList, searchText, searchByKey) {
        fieldList.filter { field ->
            if (searchByKey) {
                field.key.contains(searchText, ignoreCase = true)
            } else {
                field.keyAlias.orEmpty().contains(searchText, ignoreCase = true)
            }
        }.toMutableList()
    }

    // Collect VM events exactly once
    LaunchedEffect(Unit) {
        fieldViewModel.events.collect { ev ->
            when (ev) {
                is FieldEvent.Saved -> {
                    if (lastFlow == AddFlow.COPY) {
                        openAddFieldCopyDialog = false
                        snackbarMessage = "Copied field '${lastSubmittedKey.orEmpty()}' added"
                    } else {
                        openAddDialog = false
                        snackbarMessage = "Field '${lastSubmittedKey.orEmpty()}' added"
                    }
                    lastFlow = AddFlow.NONE
                }
                is FieldEvent.AlreadyExists -> {
                    snackbarMessage =
                        if (lastFlow == AddFlow.COPY)
                            "Copied field '${lastSubmittedKey.orEmpty()}' already exists"
                        else
                            "Field '${lastSubmittedKey.orEmpty()}' already exists"
                    lastFlow = AddFlow.NONE
                }
                is FieldEvent.Deleted -> {
                    snackbarMessage = "Deleted fields"
                }
                is FieldEvent.ValueUpdated -> {
                    snackbarMessage = "Value updated for '${ev.key}'"
                }
                is FieldEvent.AliasUpdated -> {
                    snackbarMessage = "Alias updated for '${ev.key}'"
                }
                is FieldEvent.TagUpdated -> {
                    snackbarMessage = "Tag updated for '${ev.key}'"
                }
                is FieldEvent.Error -> {
                    snackbarMessage = "Error: ${ev.throwable.message}"
                    lastFlow = AddFlow.NONE
                }
            }
        }
    }

    // Show snackbars driven by state
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    // Reset transient UI state
    fun clearStates() {
        editingField = null
        editedValue = ""
        editedAlias = ""
        searchText = ""
        filteredFields.clear()
    }

    // Persist selection on lifecycle stop
    val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)
    DisposableEffect(lifecycleOwner.value) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                val currentFilteredFields = fieldList
                    .filter {
                        if (searchByKey)
                            it.key.contains(searchText, ignoreCase = true)
                        else
                            it.keyAlias.orEmpty().contains(searchText, ignoreCase = true)
                    }
                    .filter { it in selectedFields }

                fieldViewModel.setSelectedFields(currentFilteredFields)
                clearStates()
            }
        }
        val lifecycle = lifecycleOwner.value.lifecycle
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Fields") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                expandedHeight = 30.dp
            )
        },
        floatingActionButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
            ) {
                // Back button
                GoBackButton(navController)

                // Add row button
                FloatingActionButton(onClick = { openAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Field")
                }

                // Delete selected rows button
                val isEnabled = selectedFields.isNotEmpty()
                FloatingActionButton(
                    onClick = {
                        if (isEnabled) {
                            // Delete selected fields sequentially
                            selectedFields.toList().forEach { field ->
                                fieldViewModel.deleteField(field)
                            }
                            fieldViewModel.clearSelectedKeys()
                            snackbarMessage = "Deleted fields"
                        }
                    },
                    containerColor = if (isEnabled) MaterialTheme.colorScheme.primary else Color.Gray,
                    contentColor = if (isEnabled) Color.White else Color.LightGray,
                    modifier = Modifier.alpha(if (isEnabled) 1f else 0.6f)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Selected")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(4.dp)
                .fillMaxWidth()
                .fillMaxHeight(0.90f),
            horizontalAlignment = Alignment.Start,
        ) {
            // Search header
            RowSearcher(
                searchText,
                onSearchTextChange = { searchText = it },
                searchByFirstColumn = searchByKey,
                onSearchByChange = { searchByKey = it },
                Pair("key", "alias")
            )

            if (filteredFields.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    itemsIndexed(
                        items = filteredFields,
                        key = { _, field -> field.key }
                    ) { index, field ->
                        val isSelected = selectedFields.contains(field)
                        val canAlternateColor = index % 2 == 0

                        // Use domain tag directly (no string roundtrip)
                        val backgroundColor = when {
                            isSelected -> MaterialTheme.colorScheme.primary
                            canAlternateColor -> tagColor(field.tag)
                            else -> tagColor(field.tag)
                        }

                        SelectableRow(
                            item = field,
                            background = backgroundColor,
                            onToggle = { fieldViewModel.toggleFieldSelection(field.key) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 0.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FieldItemRow(
                                    field = field,
                                    onEditClick = {
                                        editingField = field
                                        editedValue = field.value
                                    },
                                    onAddItemCopyClick = {
                                        openAddFieldCopyDialog = true
                                        targetAddFieldCopy = field
                                    },
                                )
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No fields available", style = MaterialTheme.typography.bodyMedium)
                }
            }

            HorizontalDivider(thickness = 1.dp, color = Color.Gray)
        }
    }

    // --- Add Field Dialog (creates a new domain field) ---
    if (openAddDialog) {
        AddFieldDialog(
            onDismiss = { openAddDialog = false },
            onAddField = { newField ->
                lastFlow = AddFlow.ADD
                lastSubmittedKey = newField.key

                fieldViewModel.addField(newField) // VM orchestrates: custom tag + save + events + refresh
            },
            allTags = availableTags
        )
    }

// --- Add Copy Field Dialog (duplicates with possible edits) ---
    if (openAddFieldCopyDialog && targetAddFieldCopy != null) {
        val target = targetAddFieldCopy!!
        AddCopyFieldDialog(
            targetField = target,
            allTags = availableTags,
            onDismiss = { openAddFieldCopyDialog = false },
            onAddField = { newCopyDomain ->
                lastFlow = AddFlow.COPY
                lastSubmittedKey = newCopyDomain.key
                // One simple call: ViewModel orchestrates everything (custom tag + save + events)
                fieldViewModel.addField(newCopyDomain)
            }
        )
    }

    // --- Edit dialog (value & alias) ---
    editingField?.let { field ->
        AlertDialog(
            onDismissRequest = { editingField = null },
            title = { Text("Update ${field.key}") },
            text = {
                Column {
                    // DateUtils now expects millis → convert from Instant
                    val formattedDate = DateUtils
                        .formatTimeMillis(field.dateAdded.toEpochMilli())
                        .split(" ")[0]
                    Text("Added in $formattedDate")

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = editedValue,
                        onValueChange = { editedValue = it },
                        label = { Text("New Value") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = editedAlias,
                        onValueChange = { editedAlias = it },
                        label = { Text("Alias") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    // Single call; ViewModel handles coroutines and refresh internally
                    fieldViewModel.updateField(field, editedValue, editedAlias)
                    editingField = null
                    snackbarMessage = "Field '${field.key}' updated"
                }) { Text("Accept") }
            },
            dismissButton = {
                TextButton(onClick = { editingField = null }) { Text("Cancel") }
            }
        )
    }
}
