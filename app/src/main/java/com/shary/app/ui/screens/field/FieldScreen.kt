package com.shary.app.ui.screens.field

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Details
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.*
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.shary.app.core.domain.interfaces.events.FieldEvent
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.models.reset
import com.shary.app.core.domain.types.enums.AddFlow
import com.shary.app.core.domain.types.enums.SearchFieldBy
import com.shary.app.core.domain.types.enums.safeColor
import com.shary.app.core.domain.types.enums.safeTagString
import com.shary.app.ui.screens.field.components.AddCopiedFieldDialog
import com.shary.app.ui.screens.field.components.AddFieldDialog
import com.shary.app.ui.screens.field.components.SortMenu
import com.shary.app.ui.screens.field.components.UpdateFieldDialog
import com.shary.app.ui.screens.utils.SpecialComponents.CompactActionButton
import com.shary.app.ui.screens.home.utils.Screen
import com.shary.app.ui.screens.utils.LoadingOverlay
import com.shary.app.ui.screens.utils.RowSearcher
import com.shary.app.ui.theme.ThemeMenuButton
import com.shary.app.viewmodels.configuration.ThemeViewModel
import com.shary.app.viewmodels.field.FieldViewModel
import com.shary.app.viewmodels.user.UserViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FieldsScreen(navController: NavHostController) {

    // ---------------- ViewModels ----------------
    val fieldViewModel: FieldViewModel = hiltViewModel()
    val userViewModel: UserViewModel = hiltViewModel()
    val themeViewModel: ThemeViewModel = hiltViewModel()

    // ---- Table and DB rows (Domain) ----
    val snackbarHostState = remember { SnackbarHostState() }

    // ---- Add dialogs ----
    var openAddDialog by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var lastSubmittedKey by remember { mutableStateOf<String?>(null) }
    var lastFlow by remember { mutableStateOf(AddFlow.NONE) }

    // ---- Add Copy dialog ----
    var openAddFieldCopyDialog by remember { mutableStateOf(false) }

    // ---- Editing/Updating field (Domain) ----
    var editingField by remember { mutableStateOf<FieldDomain?>(FieldDomain.initialize()) }
    var openUpdateFieldDialog by remember { mutableStateOf(false) }

    // ---- Checked rows (Domain) ----
    val selectedFields by fieldViewModel.selectedFields.collectAsState()

    // ---- Search Fields ----
    var searchQuery by remember { mutableStateOf("") }
    val searchFieldBy by fieldViewModel.searchFieldBy.collectAsState()

    val filteredFields by fieldViewModel.filteredFields.collectAsState()

    // ======== Sort Fields Parameters ========
    val sortBy by fieldViewModel.sortByParameter.collectAsState()
    val ascendingSortByMap by fieldViewModel.ascendingSortByMap.collectAsState()

    val isLoading by fieldViewModel.isLoading.collectAsState()

    // Collect VM events exactly once
    LaunchedEffect(Unit) {
        fieldViewModel.refreshFields()
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
                    snackbarMessage = "An error occurred"
                }
                is FieldEvent.MultiDeleted -> {
                    snackbarMessage = "Deleted fields"
                }
                is FieldEvent.FetchError -> {
                    snackbarMessage = "Fetch error"
                }
                is FieldEvent.NoNewFields -> {
                    snackbarMessage = "No fields to fetch"
                }
                else -> {}
            }
        }
    }

    // Show snack bars driven by state
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = ""
        }
    }

    // Reset transient UI state
    fun clearEphemeralStates() {
        editingField?.reset()
        searchQuery = ""
    }

    // Persist selection on lifecycle stop, and refresh on start
    val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)

    DisposableEffect(lifecycleOwner.value) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> fieldViewModel.refreshFields()
                Lifecycle.Event.ON_STOP -> {
                    fieldViewModel.setSelectedFields()
                    clearEphemeralStates()
                }
                else -> {}
            }
        }
        val lifecycle = lifecycleOwner.value.lifecycle
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    // ---------------- UI ----------------
    LoadingOverlay(isLoading = isLoading) {
        Scaffold(
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp)
                ) {
                    // Top row with Title, Sort and Menu
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Fields",
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.weight(1f)
                        )

                        SortMenu(
                            currentSort = sortBy,
                            isAscendingMap = ascendingSortByMap,
                            onSortChange = { s, asc ->
                                fieldViewModel.updateSort(s, asc)
                            }
                        )

                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { expanded = true }) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = "Menu"
                                )
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }) {
                                DropdownMenuItem(
                                    text = {
                                        Row() {
                                            Text("Theme")
                                            ThemeMenuButton(
                                                onThemeChosen = { theme ->
                                                    themeViewModel.updateTheme(theme)
                                                }
                                            )
                                        }
                                    },
                                    onClick = { expanded = false },
                                )

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

                    // Search bar - always visible, integrated into top bar
                    RowSearcher(
                        queryText = searchQuery,
                        onQueryTextChange = { searchQuery = it },
                        currentAttribute = searchFieldBy,
                        onAttributeChange = { fieldViewModel.updateSearchField(it) },
                        availableAttributes = SearchFieldBy.entries,
                        resolveOptionText = {
                            fieldViewModel.updateSearchField(it);
                            searchFieldBy.name
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
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

                    // ======== Center: Add + Download + Users ========

                    Row(
                        modifier = Modifier.weight(0.70f),
                        horizontalArrangement = Arrangement.spacedBy(
                            24.dp,
                            Alignment.CenterHorizontally
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CompactActionButton(
                            onClick = { openAddDialog = true },
                            icon = Icons.Default.Add,
                            backgroundColor = colorScheme.primary,
                            contentDescription = "Add Field",
                            useExtendedColors = false
                        )

                        CompactActionButton(
                            onClick = {
                                val ownerEmail = userViewModel.getOwner().email
                                if (ownerEmail.isNotEmpty()) {
                                    fieldViewModel.fetchFieldsFromCloud(ownerEmail)
                                } else {
                                    snackbarMessage = "User not logged in"
                                }
                            },
                            icon = Icons.Default.CloudDownload,
                            backgroundColor = colorScheme.primary,
                            contentDescription = "Fetch Fields from Cloud",
                            useExtendedColors = false
                        )

                        CompactActionButton(
                            onClick = { navController.navigate(Screen.Users.route) },
                            icon = Icons.Default.Person,
                            backgroundColor = colorScheme.primary,
                            contentDescription = "Send to Users",
                            useExtendedColors = false
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
                                if (userViewModel.anyCachedUser() && selectedFields.isNotEmpty()) {
                                    navController.navigate(Screen.SummaryField.route)
                                }
                            },
                            icon = Icons.Default.AssignmentTurnedIn,
                            backgroundColor = colorScheme.tertiary,
                            contentDescription = "Summary",
                            enabled = selectedFields.isNotEmpty() && userViewModel.anyCachedUser(),
                            useExtendedColors = false
                        )
                    }
                }
            },
            snackbarHost = {
                SnackbarHost(
                    snackbarHostState,
                    modifier = Modifier.background(colorScheme.inverseSurface)
                )
            }
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

                            ElevatedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .border(
                                        width = if (selectedFields.contains(field)) 6.dp else 4.dp,
                                        color = if (selectedFields.contains(field)) colorScheme.primary else field.tag.safeColor(),
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
                                            //color = field.tag.safeColor().copy(alpha = 1.0f),
                                            color = colorScheme.onSurfaceVariant,
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
                                                        Icon(
                                                            Icons.Default.ContentCopy,
                                                            contentDescription = "Copy Field Content"
                                                        )
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
                                                        Icon(
                                                            Icons.Default.CopyAll,
                                                            contentDescription = "Add Field from User Content"
                                                        )
                                                        Text("Add Copy")
                                                    }
                                                },
                                                onClick = {
                                                    expanded = false
                                                    openAddFieldCopyDialog = true
                                                    //targetAddFieldCopy = field
                                                    editingField = field.copy()
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = {
                                                    Row {
                                                        Icon(
                                                            Icons.Default.Edit,
                                                            contentDescription = "Edit Field Content"
                                                        )
                                                        Text("Edit")
                                                    }
                                                },
                                                onClick = {
                                                    expanded = false
                                                    openUpdateFieldDialog = true
                                                    editingField = field.copy()
                                                }
                                            )
                                            if (field.keyAlias.isNotEmpty()) {
                                                DropdownMenuItem(
                                                    text = {
                                                        Row {
                                                            Icon(
                                                                Icons.Default.Details,
                                                                contentDescription = "Edit Field Content"
                                                            )
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
    if (openAddFieldCopyDialog) {
        AddCopiedFieldDialog(
            targetField = editingField!!,
            onDismiss = { openAddFieldCopyDialog = false },
            onAddCopiedField = { newCopyDomain ->
                lastFlow = AddFlow.COPY
                lastSubmittedKey = newCopyDomain.key
                // One simple call: ViewModel orchestrates everything (custom tag + save + events)
                fieldViewModel.addField(newCopyDomain)
                openAddFieldCopyDialog = false
            }
        )
    }

    // --- Edit dialog (value & alias) ---
    if (openUpdateFieldDialog) {
        editingField?.let { field ->
            UpdateFieldDialog(
                targetField = field,
                onDismiss = {
                    editingField = null
                    openUpdateFieldDialog = false
                            },
                onUpdateField = { newField ->
                    lastFlow = AddFlow.UPDATE

                    // Update original creation date
                    fieldViewModel.updateField(field, newField.copy(dateAdded = field.dateAdded))
                    snackbarMessage = "Field '${field.key}' updated"
                    editingField = null
                    lastSubmittedKey = field.key
                    openUpdateFieldDialog = false
                }
            )
        }
    }
}
