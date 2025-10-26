package com.shary.app.ui.screens.field

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Details
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lens
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.types.enums.AddFlow
import com.shary.app.core.domain.types.enums.SearchFieldBy
import com.shary.app.core.domain.types.enums.Tag
import com.shary.app.core.domain.types.enums.safeColor
import com.shary.app.core.domain.types.enums.safeTagString
import com.shary.app.ui.screens.field.components.TagPicker
import com.shary.app.ui.screens.field.components.AddCopyFieldDialog
import com.shary.app.ui.screens.field.components.AddFieldDialog
import com.shary.app.ui.screens.field.components.SortMenu
import com.shary.app.ui.screens.utils.SpecialComponents.CompactActionButton
import com.shary.app.ui.screens.home.utils.Screen
import com.shary.app.ui.screens.utils.RowSearcher
import com.shary.app.utils.DateUtils
import com.shary.app.viewmodels.field.FieldEvent
import com.shary.app.viewmodels.field.FieldViewModel
import com.shary.app.viewmodels.user.UserViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FieldsScreen(navController: NavHostController) {

    // ---------------- ViewModels ----------------
    val fieldViewModel: FieldViewModel = hiltViewModel()
    val userViewModel: UserViewModel = hiltViewModel()

    // ---- Table and DB rows (Domain) ----
    //val fieldList by fieldViewModel.fields.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // ---- Add Searcher input cell ----
    var showSearch by remember { mutableStateOf(false) }

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
    var editedTag: Tag by remember { mutableStateOf(Tag.Unknown) }

    // ---- Checked rows (Domain) ----
    //val selectedFields by fieldViewModel.selectedFields.collectAsState()
    val selectedFields by fieldViewModel.selectedFields.collectAsState()

    // ---- Search Fields ----
    var searchQuery by remember { mutableStateOf("") }
    val searchFieldBy by fieldViewModel.searchFieldBy.collectAsState()

    // Safe filtering (keyAlias is nullable in domain)
    //val filteredFields = remember(fieldList, searchQuery, searchFieldBy) {
    //    fieldList.filter { it.matchBy(searchQuery, searchFieldBy) }.toMutableList()
    //}
    val filteredFields by fieldViewModel.filteredFields.collectAsState()

    // ======== Sort Fields Parameters ========
    val sortBy by fieldViewModel.sortFieldBy.collectAsState()
    val descending by fieldViewModel.descending.collectAsState()

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
                    snackbarMessage = "Deleted field"
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
                    snackbarMessage = "Deleted fields"
                }
                is FieldEvent.MultiDeleted -> {
                    snackbarMessage = "Deleted fields"
                }
            }
        }
    }

    // Show snack bars driven by state
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    // Reset transient UI state
    fun clearEphemeralStates() {
        editingField = null
        editedValue = ""
        editedAlias = ""
        editedTag = Tag.Unknown
        searchQuery = ""
        //filteredFields.clear()
    }

    // Persist selection on lifecycle stop
    val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)

    DisposableEffect(lifecycleOwner.value) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP)
            {
                fieldViewModel.setSelectedFields()
                clearEphemeralStates()
            }
        }
        val lifecycle = lifecycleOwner.value.lifecycle
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    // ---------------- UI ----------------
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 64.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                var expanded by remember { mutableStateOf(false) }

                if (showSearch) {
                    // ---- FieldSearcher below the buttons ----
                    AnimatedVisibility(
                        visible = true,
                        modifier = Modifier.weight(1f))
                    {
                        RowSearcher(
                            queryText = searchQuery,
                            onQueryTextChange = { searchQuery = it },
                            currentAttribute = searchFieldBy,
                            onAttributeChange = { fieldViewModel.updateSearchField(it) },
                            availableAttributes = SearchFieldBy.entries,
                            resolveOptionText = {
                                fieldViewModel.updateSearchField(it);
                                searchFieldBy.name
                            }
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                SortMenu(
                    currentSort = sortBy,
                    isDescending = descending,
                    onSortChange = { s, desc ->
                        fieldViewModel.updateSort(s, desc)
                    }
                )
                Box {
                    IconButton(
                        onClick = { expanded = true },
                        //modifier = Modifier.padding(start=32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Menu"
                        )
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Request") },
                            onClick = {
                                expanded = false
                                navController.navigate(Screen.Requests.route)
                            },
                            leadingIcon = { Icon(Icons.Default.Description, null) }
                        )

                        DropdownMenuItem(
                            text = { Text("File Visualization") },
                            onClick = {
                                expanded = false
                                navController.navigate(Screen.FileVisualizer.route)
                            },
                            leadingIcon = { Icon(Icons.Default.FolderOpen, null) }
                        )

                        DropdownMenuItem(
                            text = { Text("Logout") },
                            onClick = {
                                expanded = false
                                fieldViewModel.clearSelectedFields()
                                navController.navigate(Screen.Login.route)
                            },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, null) }
                        )
                    }
                }
            }
        },

        floatingActionButtonPosition = FabPosition.Center,

        // Components at the bottom
        floatingActionButton = {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                // ======== Left: Delete ========

                Box(
                    modifier = Modifier
                        .weight(0.15f),
                    contentAlignment = Alignment.Center
                ) {
                    CompactActionButton(
                        onClick = {
                            if (selectedFields.isNotEmpty()) {
                                fieldViewModel.deleteFields(selectedFields)
                                fieldViewModel.clearSelectedFields()
                                snackbarMessage = "Deleted ${selectedFields.size} fields"
                            }
                        },
                        backgroundColor = colorScheme.error,
                        icon = Icons.Default.Delete,
                        contentDescription = "Delete Fields",
                        enabled = selectedFields.isNotEmpty()
                    )
                }

                // ======== Center: Add + Search bar + Users ========

                Row(
                    modifier = Modifier.weight(0.70f),
                    horizontalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompactActionButton(
                        onClick = { openAddDialog = true },
                        icon = Icons.Default.Add,
                        backgroundColor = colorScheme.primary,
                        contentDescription = "Add Field"
                    )

                    CompactActionButton(
                        onClick = { showSearch = !showSearch },
                        icon = Icons.Default.Lens,
                        backgroundColor = colorScheme.primary,
                        contentDescription = "Show Search Bar"
                    )

                    CompactActionButton(
                        onClick = { navController.navigate(Screen.Users.route) },
                        icon = Icons.Default.Person,
                        backgroundColor = colorScheme.primary,
                        contentDescription = "Send to Users"
                    )
                }

                // ======== Right: Summary ========

                Box(
                    modifier = Modifier
                        .weight(0.15f),
                    contentAlignment = Alignment.Center
                ) {
                    CompactActionButton(
                        onClick = {
                            if (userViewModel.anyUserCached() && selectedFields.isNotEmpty()) {
                                navController.navigate(Screen.Summary.route)
                            }
                        },
                        icon = Icons.Default.AssignmentTurnedIn,
                        backgroundColor = colorScheme.tertiary,
                        contentDescription = "Summary",
                        enabled = selectedFields.isNotEmpty() && userViewModel.anyUserCached()
                    )
                }
            }
        },
        snackbarHost = {
            SnackbarHost(
                snackbarHostState,
                modifier = Modifier.background(colorScheme.inverseSurface)
        ) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxWidth(),
                //.fillMaxHeight(0.9f),
            horizontalAlignment = Alignment.Start,
        ) {

            //if (filteredFields.isNotEmpty()) {
            if (fieldViewModel.isFilteredFieldsNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                        //.padding(start = 16.dp, end = 16.dp),
                ) {
                    itemsIndexed(
                        items = filteredFields,
                        key = { _, field -> field.key }
                    ) { _, field ->
                        val isSelected = field in selectedFields // Compare by key

                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .border(
                                    width = if (isSelected) 6.dp else 4.dp,
                                    color = if (isSelected) {
                                        colorScheme.primary
                                    }
                                    else{
                                        Log.d("FieldsScreen()", "field : ${field.tag.safeColor()}")
                                        field.tag.safeColor()
                                    },
                                    shape = MaterialTheme.shapes.medium
                                ),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = colorScheme.surface
                            ),
                            onClick = { fieldViewModel.toggleFieldSelection(field) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {

                                    // key text
                                    Text(
                                        field.key,
                                        color = colorScheme.onSurface,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1
                                    )
                                    // value text
                                    Text(
                                        field.value,
                                        color = colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1
                                    )
                                    // tag text
                                    Text(
                                        "Tag: ${field.tag.safeTagString()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = field.tag.safeColor().copy(alpha = 1.0f),
                                        maxLines = 1
                                    )
                                }

                                // --- Three dots menu ---
                                var expanded by remember { mutableStateOf(false) }
                                Box {
                                    IconButton(onClick = { expanded = true }) {
                                        Icon(
                                            imageVector = Icons.Default.MoreHoriz,
                                            contentDescription = "Options"
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = {
                                                Row {
                                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Field Content")
                                                    Text("Copy")
                                                }
                                                   },
                                            onClick = {
                                                expanded = false
                                                // Direct copy to clipboard or VM
                                                snackbarMessage = "Field '${field.key}' copied"
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = {
                                                Row {
                                                    Icon(Icons.Default.CopyAll, contentDescription = "Add Field from User Content")
                                                    Text("Add Copy")
                                                }
                                                   },
                                            onClick = {
                                                expanded = false
                                                openAddFieldCopyDialog = true
                                                targetAddFieldCopy = field
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = {
                                                Row {
                                                    Icon(Icons.Default.Edit, contentDescription = "Edit Field Content")
                                                    Text("Edit")
                                                }
                                                   },
                                            onClick = {
                                                expanded = false
                                                editingField = field
                                                editedValue = field.value
                                                editedAlias = field.keyAlias.orEmpty()
                                                editedTag = field.tag
                                            }
                                        )
                                        if (!field.keyAlias.isNullOrEmpty()) {
                                            DropdownMenuItem(
                                                text = {
                                                    Row {
                                                        Icon(Icons.Default.Details, contentDescription = "Edit Field Content")
                                                        Text("Details")
                                                    }
                                                       },
                                                onClick = {
                                                    expanded = false
                                                    // Show details about the key
                                                    snackbarMessage = field.keyAlias
                                                }
                                            )
                                        }
                                    }
                                }
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
        )
    }

    // --- Add Copy Field Dialog (duplicates with possible edits) ---
    if (openAddFieldCopyDialog && targetAddFieldCopy != null) {
        val target = targetAddFieldCopy!!
        AddCopyFieldDialog(
            targetField = target,
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

                    Spacer(Modifier.height(8.dp))

                    // --- Tag picker (expects String; convert to/from UiFieldTag) ---
                    TagPicker(
                        selectedTag = editingField!!.tag,
                        onTagSelected = { selectedTag ->
                            editingField = editingField?.copy(tag = selectedTag)
                            editedTag = selectedTag // <-- keep local state in sync!
                        },
                    )

                    Spacer(Modifier.height(16.dp))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    // Single call; ViewModel handles coroutines and refresh internally
                    Log.w("FieldsScreen", "before update: $editedTag")
                    fieldViewModel.updateField(field,
                        editedValue,
                        editedAlias,
                        editingField!!.tag
                    )
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
