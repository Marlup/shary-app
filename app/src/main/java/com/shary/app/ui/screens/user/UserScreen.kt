package com.shary.app.ui.screens.user

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.shary.app.core.domain.interfaces.events.FieldEvent
import com.shary.app.core.domain.models.UserDomain
import com.shary.app.core.domain.types.enums.ContactSortByParameter
import com.shary.app.ui.components.ContactsToolbar
import com.shary.app.ui.components.DockAction
import com.shary.app.ui.components.DockVisualStyle
import com.shary.app.ui.components.EmptyState
import com.shary.app.ui.components.PrimaryTabOverflowMenu
import com.shary.app.ui.components.SharyBottomActionShelf
import com.shary.app.ui.components.SharyCommandDock
import com.shary.app.ui.components.SharyModalAction
import com.shary.app.ui.components.SharySoftButton
import com.shary.app.ui.components.SharySectionNavigationBar
import com.shary.app.ui.components.SectionTab
import com.shary.app.ui.components.UserCard
import com.shary.app.ui.screens.field.components.ChangePasswordDialog
import com.shary.app.ui.screens.home.utils.Screen
import com.shary.app.ui.screens.SendFlowDestination
import com.shary.app.ui.screens.userSendFlowCta
import com.shary.app.ui.screens.user.components.AddCopyUserDialog
import com.shary.app.ui.screens.user.components.AddUserDialog
import com.shary.app.ui.screens.user.components.EditContactTagDialog
import com.shary.app.ui.screens.user.components.UserDetailDialog
import com.shary.app.ui.screens.user.components.UserEditorDialog
import com.shary.app.ui.theme.SharyRadius
import com.shary.app.ui.theme.Violet50
import com.shary.app.ui.theme.Violet600
import com.shary.app.ui.theme.Violet800
import com.shary.app.ui.utils.CopyFeedback
import com.shary.app.viewmodels.configuration.SettingsViewModel
import com.shary.app.viewmodels.clipboard.ClipboardViewModel
import com.shary.app.viewmodels.field.FieldViewModel
import com.shary.app.viewmodels.request.RequestViewModel
import com.shary.app.viewmodels.user.ContactEditorOperation
import com.shary.app.viewmodels.user.ContactEditorOperationState
import com.shary.app.viewmodels.user.ContactTagViewModel
import com.shary.app.viewmodels.user.UserEvent
import com.shary.app.viewmodels.user.UserViewModel
import com.shary.app.viewmodels.user.filterUsersByTag
import kotlinx.coroutines.launch

@Suppress("ASSIGNED_VALUE_IS_NEVER_READ", "UNUSED_VALUE")
@Composable
fun UsersScreen(
    navController: NavHostController,
    onLogout: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val userViewModel: UserViewModel = hiltViewModel()
    val fieldViewModel: FieldViewModel = hiltViewModel()
    val requestViewModel: RequestViewModel = hiltViewModel()
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val clipboardViewModel: ClipboardViewModel = hiltViewModel()

    val userList by userViewModel.users.collectAsState()
    val selectedUsers by userViewModel.selectedUsers.collectAsState()
    val isLoading by userViewModel.isLoading.collectAsState()
    val editorOperationState by userViewModel.editorOperationState.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()
    val filteredUsers by userViewModel.filteredUsers.collectAsState()
    val sortBy by userViewModel.sortByParameter.collectAsState()
    val ascendingSortByMap by userViewModel.ascendingSortByMap.collectAsState()

    val contactTagViewModel: ContactTagViewModel = hiltViewModel()
    val contactTags by contactTagViewModel.tags.collectAsState()
    val contactTagEditorState by contactTagViewModel.editorOperationState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var openAddDialog by remember { mutableStateOf(false) }
    var openAddUserCopyDialog by remember { mutableStateOf(false) }
    var openEditDialog by remember { mutableStateOf(false) }
    var targetAddUserCopy by remember { mutableStateOf<UserDomain?>(null) }
    var editingUser by remember { mutableStateOf<UserDomain?>(null) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf("All") }
    var sortExpanded by remember { mutableStateOf(false) }
    var openChangePasswordDialog by remember { mutableStateOf(false) }
    var detailUserEmail by rememberSaveable { mutableStateOf<String?>(null) }
    var quickActionUser by remember { mutableStateOf<UserDomain?>(null) }
    var openEditTagDialog by remember { mutableStateOf(false) }
    val editorIsSaving = editorOperationState is ContactEditorOperationState.Saving
    val editorErrorMessage = (editorOperationState as? ContactEditorOperationState.Failed)?.message

    val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)

    LaunchedEffect(Unit) {
        userViewModel.events.collect { ev ->
            when (ev) {
                is UserEvent.Saved -> snackbarMessage = "Contact saved"
                is UserEvent.AlreadyExists -> snackbarMessage = "A contact with this email already exists"
                is UserEvent.Deleted -> snackbarMessage = "Deleted contact '${ev.email}'"
                is UserEvent.Error -> snackbarMessage = "Error: ${ev.throwable.message}"
            }
            userViewModel.refreshUsers()
        }
    }

    LaunchedEffect(editorOperationState) {
        val terminal = editorOperationState as? ContactEditorOperationState.Saved
            ?: return@LaunchedEffect
        when (terminal.operation) {
            ContactEditorOperation.ADD -> {
                openAddDialog = false
                openAddUserCopyDialog = false
                targetAddUserCopy = null
            }
            ContactEditorOperation.UPDATE -> {
                openEditDialog = false
                editingUser = null
            }
        }
        userViewModel.resetEditorOperationState()
    }

    LaunchedEffect(Unit) {
        fieldViewModel.events.collect { event ->
            when (event) {
                FieldEvent.PasswordChanged -> {
                    openChangePasswordDialog = false
                    navController.navigate(Screen.Login.routeWithPasswordChanged(true)) {
                        popUpTo(Screen.Fields.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
                is FieldEvent.Error -> {
                    if (openChangePasswordDialog) {
                        snackbarMessage = event.throwable.message ?: "Unable to change password"
                    }
                }
                else -> Unit
            }
        }
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    LaunchedEffect(searchQuery) {
        userViewModel.updateSearch(searchQuery)
    }

    DisposableEffect(lifecycleOwner.value) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> userViewModel.refreshUsers()
                Lifecycle.Event.ON_STOP -> {
                    userViewModel.cacheUsers(selectedUsers)
                    searchQuery = ""
                    activeFilter = "All"
                    quickActionUser = null
                    openEditTagDialog = false
                }
                else -> Unit
            }
        }
        lifecycleOwner.value.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.value.lifecycle.removeObserver(observer) }
    }

    val dynamicContactTagFilters = remember(userList) {
        userList
            .map { it.tag.toTagString().trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .sortedBy { it.lowercase() }
    }
    val searchFilters = remember(dynamicContactTagFilters) {
        listOf("All") + dynamicContactTagFilters
    }

    LaunchedEffect(searchFilters) {
        if (activeFilter !in searchFilters) {
            activeFilter = "All"
        }
    }

    val visibleUsers = remember(filteredUsers, activeFilter) {
        filterUsersByTag(filteredUsers, activeFilter)
    }

    val selectedSingle = selectedUsers.singleOrNull()
    val detailUser = remember(userList, detailUserEmail) {
        val normalizedEmail = detailUserEmail?.trim()?.lowercase()
        userList.firstOrNull { user ->
            user.email.trim().lowercase() == normalizedEmail
        }
    }
    val cardVerticalPadding = if (settings.compactListMode) 2.dp else 4.dp

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(Violet50),
        topBar = {
            Column(modifier = Modifier.statusBarsPadding()) {
                ContactsToolbar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    filters = searchFilters,
                    activeFilter = activeFilter,
                    onFilterChange = { activeFilter = it },
                    sortAction = {
                        Box {
                            SharySoftButton(
                                text = sortBy.label,
                                icon = Icons.Default.SwapVert,
                                modifier = Modifier
                                    .width(54.dp)
                                    .border(1.5.dp, Violet600, SharyRadius.input),
                                minHeight = 40.dp,
                                iconSize = 13.dp,
                                iconSpacing = 2.dp,
                                contentPadding = PaddingValues(horizontal = 5.dp, vertical = 0.dp),
                                containerColor = Color.White,
                                contentColor = Violet800,
                                onClick = { sortExpanded = true }
                            )
                            DropdownMenu(
                                expanded = sortExpanded,
                                onDismissRequest = { sortExpanded = false }
                            ) {
                                ContactSortByParameter.entries.forEach { option ->
                                    val isAscending = ascendingSortByMap[option] ?: true
                                    val isSelected = option == sortBy
                                    DropdownMenuItem(
                                        text = { Text("${option.label} ${if (isAscending) "↑" else "↓"}") },
                                        onClick = {
                                            val nextAscending =
                                                if (isSelected) !isAscending else isAscending
                                            userViewModel.updateSort(option, nextAscending)
                                            sortExpanded = false
                                        },
                                        leadingIcon = if (isSelected) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null
                                                )
                                            }
                                        } else {
                                            null
                                        }
                                    )
                                }
                            }
                        }
                    },
                    overflowAction = {
                        PrimaryTabOverflowMenu(
                            onOpenSettings = { navController.navigate(Screen.Settings.route) },
                            onChangePassword = { openChangePasswordDialog = true },
                            onLogout = {
                                fieldViewModel.clearSelectedFields()
                                onLogout()
                            }
                        )
                    }
                )
            }
        },
        bottomBar = {
            Column {
                if (selectedUsers.isNotEmpty()) {
                    val sendFlowCta = userSendFlowCta(
                        hasSelectedUsers = true,
                        hasSelectedFields = fieldViewModel.anyFieldCached(),
                        hasSelectedRequestFields = requestViewModel.anyDraftFieldCached()
                    )
                    val currentSendFlowCta = requireNotNull(sendFlowCta)
                    SharyCommandDock(
                        selectedCount = selectedUsers.size,
                        onClearSelection = userViewModel::clearSelectedUsers,
                        primaryAction = DockAction(
                            label = currentSendFlowCta.label,
                            icon = when (currentSendFlowCta.destination) {
                                SendFlowDestination.FIELDS -> Icons.Default.TextFields
                                else -> Icons.AutoMirrored.Filled.ArrowForward
                            },
                            contentDescription = currentSendFlowCta.contentDescription
                        ),
                        secondaryActions = if (currentSendFlowCta.destination == SendFlowDestination.FIELDS) {
                            emptyList()
                        } else {
                            listOfNotNull(
                                selectedSingle?.let { user ->
                                    DockAction(
                                        label = "Edit",
                                        icon = Icons.Default.Edit,
                                        contentDescription = "Edit selected contact"
                                    ) {
                                        editingUser = user
                                        openEditDialog = true
                                    }
                                },
                                selectedSingle?.let { user ->
                                    DockAction(
                                        label = "Duplicate",
                                        icon = Icons.Default.ContentCopy,
                                        contentDescription = "Duplicate selected contact"
                                    ) {
                                        targetAddUserCopy = user
                                        openAddUserCopyDialog = true
                                    }
                                }
                            )
                        },
                        destructiveAction = DockAction(
                            label = "Delete",
                            icon = Icons.Default.Delete,
                            contentDescription = "Delete selected contacts"
                        ) {
                            val deletedSnapshot = selectedUsers.toList()
                            userViewModel.deleteUsers(deletedSnapshot)
                            userViewModel.clearSelectedUsers()
                            scope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                val result = snackbarHostState.showSnackbar(
                                    message = "Deleted ${deletedSnapshot.size} contacts",
                                    actionLabel = "Undo",
                                    duration = SnackbarDuration.Long
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    userViewModel.restoreDeletedUsers(deletedSnapshot)
                                }
                            }
                        },
                        visualStyle = if (currentSendFlowCta.destination == SendFlowDestination.FIELDS) {
                            DockVisualStyle.FullWidth
                        } else {
                            DockVisualStyle.IconOnly
                        },
                        onPrimaryClick = {
                            userViewModel.cacheUsers(selectedUsers)
                            when (currentSendFlowCta.destination) {
                                SendFlowDestination.FIELDS -> navController.navigate(Screen.Fields.route)
                                SendFlowDestination.FIELD_REVIEW -> navController.navigate(Screen.SummaryField.route)
                                SendFlowDestination.REQUEST_REVIEW -> {
                                    scope.launch {
                                        val requestId =
                                            requestViewModel.setOutgoingRecipients(selectedUsers)
                                        if (requestId == null) {
                                            snackbarMessage =
                                                "Unable to persist the request review draft"
                                        } else {
                                            navController.navigate(
                                                Screen.SummaryRequest.reviewRoute(requestId)
                                            )
                                        }
                                    }
                                }
                                else -> Unit
                            }
                        }
                    )
                } else {
                    SharyBottomActionShelf {
                        SharySoftButton(
                            text = "Add contact",
                            icon = Icons.Default.Add,
                            contentDescription = "Add contact",
                            modifier = Modifier.weight(1f),
                            minHeight = 52.dp,
                            containerColor = Violet600,
                            contentColor = Color.White,
                            onClick = { openAddDialog = true }
                        )
                    }
                }
                SharySectionNavigationBar(
                    currentTab = SectionTab.USERS,
                    onTabSelected = { tab ->
                        when (tab) {
                            SectionTab.FIELDS -> navController.navigate(Screen.Fields.route) {
                                launchSingleTop = true
                            }
                            SectionTab.CONNECT -> navController.navigate(Screen.Connectors.route) {
                                launchSingleTop = true
                            }
                            SectionTab.USERS -> Unit
                            SectionTab.REQUESTS -> navController.navigate(Screen.Requests.route) {
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(Violet50)
        ) {
            if (visibleUsers.isEmpty() && !isLoading) {
                val isFiltering = searchQuery.isNotBlank() || activeFilter != "All"
                EmptyState(
                    title = if (isFiltering) "No matching contacts" else "No contacts yet",
                    body = if (isFiltering) {
                        "Try a different alias, email, or group"
                    } else {
                        "Add your first contact using email and a local alias"
                    },
                    primaryAction = if (isFiltering) "Clear filters" else "Add your first contact",
                    onPrimaryAction = {
                        if (isFiltering) {
                            searchQuery = ""
                            activeFilter = "All"
                        } else {
                            openAddDialog = true
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                ) {
                    items(visibleUsers, key = { it.email }) { user ->
                        UserCard(
                            name = user.username,
                            email = user.email,
                            tag = user.tag,
                            isSelected = selectedUsers.any {
                                it.email.trim().equals(user.email.trim(), ignoreCase = true)
                            },
                            onCardTap = { detailUserEmail = user.email },
                            onToggleSelection = { userViewModel.toggleUser(user) },
                            onLongPressCopy = { email ->
                                clipboardViewModel.copyText(
                                    label = "contact_email",
                                    text = email,
                                    sensitive = false,
                                    clearAfterSeconds = settings.clipboardAutoClearSeconds
                                )
                                CopyFeedback.playSuccessTone()
                                snackbarMessage = "Contact email copied"
                            },
                            copyLongPressMillis = settings.copyLongPressMillis.toLong(),
                            modifier = Modifier.padding(vertical = cardVerticalPadding)
                        )
                    }
                }
            }
        }
    }

    detailUser?.let { user ->
        val detailFlowCta = requireNotNull(
            userSendFlowCta(
                hasSelectedUsers = true,
                hasSelectedFields = fieldViewModel.anyFieldCached(),
                hasSelectedRequestFields = requestViewModel.anyDraftFieldCached()
            )
        )
        UserDetailDialog(
            user = user,
            onDismiss = { detailUserEmail = null },
            onEdit = {
                detailUserEmail = null
                userViewModel.resetEditorOperationState()
                editingUser = user
                openEditDialog = true
            },
            onCopyEmail = {
                clipboardViewModel.copyText(
                    label = "contact_email",
                    text = user.email,
                    sensitive = false,
                    clearAfterSeconds = settings.clipboardAutoClearSeconds
                )
                CopyFeedback.playSuccessTone()
                snackbarMessage = "Contact email copied"
            },
            onCreateSimilar = {
                detailUserEmail = null
                userViewModel.resetEditorOperationState()
                targetAddUserCopy = user
                openAddUserCopyDialog = true
            },
            onDelete = {
                detailUserEmail = null
                val deletedSnapshot = listOf(user)
                userViewModel.deleteUsers(deletedSnapshot)
                if (selectedUsers.any { selected ->
                        selected.email.equals(user.email, ignoreCase = true)
                    }
                ) {
                    userViewModel.clearSelectedUsers()
                }
                scope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    val result = snackbarHostState.showSnackbar(
                        message = "Deleted 1 contact",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Long
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        userViewModel.restoreDeletedUsers(deletedSnapshot)
                    }
                }
            },
            onChangeTag = {
                detailUserEmail = null
                quickActionUser = user
                openEditTagDialog = true
            },
            flowAction = SharyModalAction(
                label = detailFlowCta.label,
                enabled = true,
                onClick = {
                    detailUserEmail = null
                    userViewModel.cacheUsers(listOf(user))
                    when (detailFlowCta.destination) {
                        SendFlowDestination.FIELDS ->
                            navController.navigate(Screen.Fields.route)
                        SendFlowDestination.FIELD_REVIEW ->
                            navController.navigate(Screen.SummaryField.route)
                        SendFlowDestination.REQUEST_REVIEW -> {
                            scope.launch {
                                val requestId =
                                    requestViewModel.setOutgoingRecipients(listOf(user))
                                if (requestId == null) {
                                    snackbarMessage =
                                        "Unable to persist the request review draft"
                                } else {
                                    navController.navigate(
                                        Screen.SummaryRequest.reviewRoute(requestId)
                                    )
                                }
                            }
                        }
                        else -> Unit
                    }
                }
            )
        )
    }

    if (openAddDialog) {
        AddUserDialog(
            onDismiss = {
                openAddDialog = false
                userViewModel.resetEditorOperationState()
            },
            onAddUser = { newUser -> userViewModel.submitEditorAddUser(newUser) },
            isSaving = editorIsSaving,
            errorMessage = editorErrorMessage,
            availableTags = contactTags,
            onCreateTag = { name, color -> contactTagViewModel.submitAddTag(name, color) },
            tagEditorState = contactTagEditorState,
            onResetTagEditorState = contactTagViewModel::resetEditorOperationState
        )
    }

    if (openAddUserCopyDialog && targetAddUserCopy != null) {
        AddCopyUserDialog(
            targetUser = targetAddUserCopy!!,
            onDismiss = {
                openAddUserCopyDialog = false
                targetAddUserCopy = null
                userViewModel.resetEditorOperationState()
            },
            onAddUser = { newUser -> userViewModel.submitEditorAddUser(newUser) },
            isSaving = editorIsSaving,
            errorMessage = editorErrorMessage,
            availableTags = contactTags,
            onCreateTag = { name, color -> contactTagViewModel.submitAddTag(name, color) },
            tagEditorState = contactTagEditorState,
            onResetTagEditorState = contactTagViewModel::resetEditorOperationState
        )
    }

    if (openEditDialog && editingUser != null) {
        UserEditorDialog(
            initial = editingUser!!,
            title = "Edit Contact",
            confirmLabel = "Save",
            onDismiss = {
                openEditDialog = false
                editingUser = null
                userViewModel.resetEditorOperationState()
            },
            onConfirm = { updated ->
                userViewModel.submitEditorUpdateUser(editingUser!!, updated)
            },
            isSaving = editorIsSaving,
            errorMessage = editorErrorMessage,
            availableTags = contactTags,
            onCreateTag = { name, color -> contactTagViewModel.submitAddTag(name, color) },
            tagEditorState = contactTagEditorState,
            onResetTagEditorState = contactTagViewModel::resetEditorOperationState
        )
    }

    if (openEditTagDialog && quickActionUser != null) {
        EditContactTagDialog(
            initialTag = quickActionUser!!.tag,
            onDismiss = {
                openEditTagDialog = false
                quickActionUser = null
            },
            onConfirm = { newTag ->
                userViewModel.updateTag(quickActionUser!!, newTag)
                openEditTagDialog = false
                quickActionUser = null
            }
        )
    }

    if (openChangePasswordDialog) {
        val isPasswordLoading by fieldViewModel.isLoading.collectAsState()
        ChangePasswordDialog(
            isLoading = isPasswordLoading,
            onDismiss = { openChangePasswordDialog = false },
            onAccept = { oldPassword, newPassword, repeatNewPassword ->
                fieldViewModel.changePasswordAndRewrapDataKey(
                    context = navController.context,
                    oldPassword = oldPassword,
                    newPassword = newPassword,
                    repeatNewPassword = repeatNewPassword
                )
            }
        )
    }
}
