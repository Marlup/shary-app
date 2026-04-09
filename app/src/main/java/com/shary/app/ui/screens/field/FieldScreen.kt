package com.shary.app.ui.screens.field

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.*
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.shary.app.core.domain.types.valueobjects.FieldValueContract
import com.shary.app.ui.screens.field.components.AddCopiedFieldDialog
import com.shary.app.ui.screens.field.components.AddFieldDialog
import com.shary.app.ui.screens.field.components.ChangePasswordDialog
import com.shary.app.ui.screens.field.components.SortMenu
import com.shary.app.ui.screens.field.components.UpdateFieldDialog
import com.shary.app.ui.screens.utils.SpecialComponents.CompactActionButton
import com.shary.app.ui.screens.home.utils.Screen
import com.shary.app.ui.screens.utils.LoadingOverlay
import com.shary.app.ui.screens.utils.LongPressHint
import com.shary.app.ui.screens.utils.RowSearcher
import com.shary.app.ui.screens.utils.ScreenScaffold
import com.shary.app.ui.theme.ThemeMenuButton
import com.shary.app.viewmodels.configuration.ThemeViewModel
import com.shary.app.viewmodels.field.FieldViewModel
import com.shary.app.viewmodels.user.UserViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FieldsScreen(navController: NavHostController) {
    val context = LocalContext.current

    // ---------------- ViewModels ----------------
    val fieldViewModel: FieldViewModel = hiltViewModel()
    val userViewModel: UserViewModel = hiltViewModel()
    val themeViewModel: ThemeViewModel = hiltViewModel()

    // ---- Table and DB rows (Domain) ----
    val snackbarHostState = remember { SnackbarHostState() }

    // ---- Add dialogs ----
    var openAddDialog by remember { mutableStateOf(false) }
    var openChangePasswordDialog by remember { mutableStateOf(false) }
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
    val recoverableKeys by fieldViewModel.recoverableKeys.collectAsState()

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
                is FieldEvent.ValueRecovered -> {
                    snackbarMessage = "Previous value recovered for '${ev.key}'"
                }
                is FieldEvent.AliasUpdated -> {
                    snackbarMessage = "Alias updated for '${ev.key}'"
                }
                is FieldEvent.TagUpdated -> {
                    snackbarMessage = "Tag updated for '${ev.key}'"
                }
                is FieldEvent.Error -> {
                    snackbarMessage = ev.throwable.message ?: "An error occurred"
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
                FieldEvent.PasswordChanged -> {
                    openChangePasswordDialog = false
                    fieldViewModel.clearSelectedFields()
                    navController.navigate(Screen.Login.routeWithPasswordChanged(true)) {
                        popUpTo(Screen.Fields.route) { inclusive = true }
                        launchSingleTop = true
                    }
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
        ScreenScaffold(
            title = "Fields",
            snackbarHostState = snackbarHostState,
            topActions = {
                SortMenu(
                    currentSort = sortBy,
                    isAscendingMap = ascendingSortByMap,
                    onSortChange = { s, asc ->
                        fieldViewModel.updateSort(s, asc)
                    }
                )

                var expanded by remember { mutableStateOf(false) }
                Box {
                    LongPressHint("Open field screen menu") {
                        IconButton(onClick = { expanded = true }) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "Menu"
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(
                            text = {
                                ThemeMenuButton(
                                    onThemeChosen = { theme ->
                                        themeViewModel.updateTheme(theme)
                                    }
                                )
                            },
                            onClick = { expanded = false },
                        )

                        DropdownMenuItem(
                            text = { Text("Change password") },
                            onClick = {
                                expanded = false
                                openChangePasswordDialog = true
                            },
                            leadingIcon = { Icon(Icons.Default.Lock, null) }
                        )

                        DropdownMenuItem(
                            text = { Text("Logout") },
                            onClick = {
                                expanded = false
                                fieldViewModel.clearSelectedFields()
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(Screen.Fields.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, null) }
                        )
                    }
                }
            },
            searchContent = {
                RowSearcher(
                    queryText = searchQuery,
                    onQueryTextChange = { searchQuery = it },
                    currentAttribute = searchFieldBy,
                    onAttributeChange = { fieldViewModel.updateSearchField(it) },
                    availableAttributes = SearchFieldBy.entries,
                    resolveOptionText = {
                        fieldViewModel.updateSearchField(it)
                        searchFieldBy.name
                    }
                )
            },
            bottomBarContent = {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
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
                        )

                        CompactActionButton(
                            onClick = { navController.navigate(Screen.Users.route) },
                            icon = Icons.Default.Person,
                            backgroundColor = colorScheme.primary,
                            contentDescription = "Send to Users",
                        )

                        CompactActionButton(
                            onClick = { navController.navigate(Screen.Requests.route) },
                            icon = Icons.Default.Description,
                            backgroundColor = colorScheme.primary,
                            contentDescription = "Go to Request Screen",
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
                            val isSelected = selectedFields.contains(field)
                            val displayValue = remember(field.value) {
                                FieldValueContract.parse(field.value).plainData
                            }

                            val stripeColor = field.tag.safeColor()

                            val actionPanelWidth = if (field.keyAlias.isNotEmpty()) 248.dp else 186.dp
                            val cardEndPadding by animateDpAsState(
                                targetValue = if (isSelected) actionPanelWidth else 0.dp,
                                label = "fieldCardEndPadding"
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            ) {
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = isSelected,
                                    modifier = Modifier.align(Alignment.CenterEnd),
                                    enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                                    exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                                ) {
                                    SwipeActionsRow(
                                        modifier = Modifier
                                            .width(actionPanelWidth)
                                            .fillMaxHeight(),
                                        onEdit = {
                                            openUpdateFieldDialog = true
                                            editingField = field.copy()
                                        },
                                        onCopy = {
                                            snackbarMessage = "Field '${field.key}' copied"
                                        },
                                        onAddCopy = {
                                            openAddFieldCopyDialog = true
                                            editingField = field.copy()
                                        },
                                        onDetails = if (field.keyAlias.isNotEmpty()) {
                                            { snackbarMessage = field.keyAlias }
                                        } else null
                                    )
                                }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(end = cardEndPadding),
                                    colors = CardDefaults.cardColors(
                                        containerColor = colorScheme.surface
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                    onClick = { fieldViewModel.toggleFieldSelection(field) }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp, horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(6.dp)
                                                .heightIn(min = 48.dp)
                                                .background(stripeColor)
                                        )

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                field.key,
                                                color = colorScheme.onSurface,
                                                style = MaterialTheme.typography.titleMedium,
                                                maxLines = 1
                                            )
                                            Text(
                                                displayValue,
                                                color = colorScheme.onSurfaceVariant,
                                                style = MaterialTheme.typography.bodyMedium,
                                                maxLines = 1
                                            )
                                            Text(
                                                field.tag.safeTagString(),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                    )
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
                },
                canRecoverPreviousValue = recoverableKeys.contains(field.key.trim().lowercase()),
                onRecoverPreviousValue = {
                    fieldViewModel.recoverPreviousValue(field)
                    editingField = null
                    openUpdateFieldDialog = false
                }
            )
        }
    }

    if (openChangePasswordDialog) {
        ChangePasswordDialog(
            isLoading = isLoading,
            onDismiss = { openChangePasswordDialog = false },
            onAccept = { oldPassword, newPassword, repeatNewPassword ->
                fieldViewModel.changePasswordAndRewrapDataKey(
                    context = context,
                    oldPassword = oldPassword,
                    newPassword = newPassword,
                    repeatNewPassword = repeatNewPassword
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeActionsRow(
    modifier: Modifier = Modifier,
    onEdit: () -> Unit,
    onCopy: () -> Unit,
    onAddCopy: () -> Unit,
    onDetails: (() -> Unit)?
) {
    Row(
        modifier = modifier
            .background(Color.Transparent)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SwipeActionButton(
            label = "",
            icon = Icons.Default.Edit,
            onClick = onEdit
        )
        SwipeActionButton(
            label = "",
            icon = Icons.Default.ContentCopy,
            onClick = onCopy
        )
        SwipeActionButton(
            label = "",
            icon = Icons.Default.CopyAll,
            onClick = onAddCopy
        )
        if (onDetails != null) {
            SwipeActionButton(
                label = "",
                icon = Icons.Default.Details,
                onClick = onDetails
            )
        }
    }
}

@Composable
private fun SwipeActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    LongPressHint("Run this quick action") {
        TextButton(onClick = onClick) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(icon, contentDescription = label)
            }
        }
    }
}
