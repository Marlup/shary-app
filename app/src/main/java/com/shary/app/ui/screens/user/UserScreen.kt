package com.shary.app.ui.screens.user

import android.content.ClipData
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.shary.app.core.domain.models.UserDomain
import com.shary.app.ui.components.DockAction
import com.shary.app.ui.components.EmptyState
import com.shary.app.ui.components.SharyCommandDock
import com.shary.app.ui.components.SharyIconButton
import com.shary.app.ui.components.SharySearchBar
import com.shary.app.ui.components.SharySectionNavigationBar
import com.shary.app.ui.components.SharyTopBar
import com.shary.app.ui.components.SectionTab
import com.shary.app.ui.components.UserCard
import com.shary.app.ui.screens.home.utils.Screen
import com.shary.app.ui.screens.user.components.AddCopyUserDialog
import com.shary.app.ui.screens.user.components.AddUserDialog
import com.shary.app.ui.screens.user.components.UserEditorDialog
import com.shary.app.ui.theme.Violet50
import com.shary.app.viewmodels.configuration.SettingsViewModel
import com.shary.app.viewmodels.field.FieldViewModel
import com.shary.app.viewmodels.request.RequestViewModel
import com.shary.app.viewmodels.user.UserEvent
import com.shary.app.viewmodels.user.UserViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun UsersScreen(navController: NavHostController) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val userViewModel: UserViewModel = hiltViewModel()
    val fieldViewModel: FieldViewModel = hiltViewModel()
    val requestViewModel: RequestViewModel = hiltViewModel()
    val settingsViewModel: SettingsViewModel = hiltViewModel()

    val userList by userViewModel.users.collectAsState()
    val selectedUsers by userViewModel.selectedUsers.collectAsState()
    val isLoading by userViewModel.isLoading.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var openAddDialog by remember { mutableStateOf(false) }
    var openAddUserCopyDialog by remember { mutableStateOf(false) }
    var openEditDialog by remember { mutableStateOf(false) }
    var targetAddUserCopy by remember { mutableStateOf<UserDomain?>(null) }
    var editingUser by remember { mutableStateOf<UserDomain?>(null) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var clipboardAutoClearJob by remember { mutableStateOf<Job?>(null) }

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

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    DisposableEffect(lifecycleOwner.value) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> userViewModel.refreshUsers()
                Lifecycle.Event.ON_STOP -> {
                    userViewModel.cacheUsers(selectedUsers)
                    searchQuery = ""
                }
                else -> Unit
            }
        }
        lifecycleOwner.value.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.value.lifecycle.removeObserver(observer) }
    }

    val filteredUsers = remember(userList, searchQuery) {
        val query = searchQuery.trim()
        if (query.isBlank()) {
            userList
        } else {
            userList.filter { user ->
                user.username.contains(query, ignoreCase = true) ||
                        user.email.contains(query, ignoreCase = true)
            }
        }
    }

    val selectedSingle = selectedUsers.singleOrNull()
    val cardVerticalPadding = if (settings.compactListMode) 2.dp else 4.dp

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(Violet50),
        topBar = {
            Column(modifier = Modifier.statusBarsPadding()) {
                SharyTopBar(
                    title = "",
                    subtitle = "${userList.size} stored",
                    actions = {
                        SharyIconButton(
                            icon = Icons.Default.Add,
                            contentDescription = "Add user",
                            onClick = { openAddDialog = true }
                        )
                        if (selectedSingle != null) {
                            SharyIconButton(
                                icon = Icons.Default.Edit,
                                contentDescription = "Edit selected user",
                                onClick = {
                                    editingUser = selectedSingle
                                    openEditDialog = true
                                }
                            )
                            SharyIconButton(
                                icon = Icons.Default.ContentCopy,
                                contentDescription = "Duplicate selected user",
                                onClick = {
                                    targetAddUserCopy = selectedSingle
                                    openAddUserCopyDialog = true
                                }
                            )
                        }
                    }
                )
                SharySearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "Search contacts by alias or email..."
                )
            }
        },
        bottomBar = {
            Column {
                SharyCommandDock(
                    selectedCount = selectedUsers.size,
                    onClearSelection = userViewModel::clearSelectedUsers,
                    primaryAction = DockAction(
                        label = "Review & Send →",
                        enabled = selectedUsers.isNotEmpty() &&
                                (fieldViewModel.anyFieldCached() || requestViewModel.anyDraftFieldCached())
                    ),
                    secondaryActions = emptyList(),
                    destructiveAction = DockAction(
                        label = "Delete",
                        icon = Icons.Default.Delete,
                        enabled = selectedUsers.isNotEmpty()
                    ) {
                        if (selectedUsers.isNotEmpty()) {
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
                        }
                    },
                    onPrimaryClick = {
                        when {
                            fieldViewModel.anyFieldCached() && selectedUsers.isNotEmpty() -> {
                                navController.navigate(Screen.SummaryField.route)
                            }
                            requestViewModel.anyDraftFieldCached() && selectedUsers.isNotEmpty() -> {
                                navController.navigate(Screen.SummaryRequest.route)
                            }
                            else -> snackbarMessage = "Select at least one recipient and payload"
                        }
                    }
                )
                SharySectionNavigationBar(
                    currentTab = SectionTab.USERS,
                    onTabSelected = { tab ->
                        when (tab) {
                            SectionTab.FIELDS -> navController.navigate(Screen.Fields.route) {
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
            if (filteredUsers.isEmpty() && !isLoading) {
                EmptyState(
                    title = "No contacts yet",
                    body = "Add your first contact using email and a local alias",
                    primaryAction = "Add your first contact",
                    onPrimaryAction = { openAddDialog = true },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                ) {
                    items(filteredUsers, key = { it.email }) { user ->
                        UserCard(
                            name = user.username,
                            email = user.email,
                            isSelected = selectedUsers.any {
                                it.email.trim().equals(user.email.trim(), ignoreCase = true)
                            },
                            onClick = { userViewModel.toggleUser(user) },
                            onLongPressCopy = { email ->
                                scope.launch {
                                    clipboard.setClipEntry(
                                        ClipEntry(ClipData.newPlainText("contact_email", email))
                                    )
                                }
                                clipboardAutoClearJob?.cancel()
                                if (settings.clipboardAutoClearSeconds > 0) {
                                    clipboardAutoClearJob = scope.launch {
                                        delay(settings.clipboardAutoClearSeconds * 1000L)
                                        clipboard.setClipEntry(
                                            ClipEntry(ClipData.newPlainText("", ""))
                                        )
                                    }
                                }
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

    if (openAddDialog) {
        AddUserDialog(
            onDismiss = { openAddDialog = false },
            onAddUser = { newUser -> userViewModel.saveUser(newUser) }
        )
    }

    if (openAddUserCopyDialog && targetAddUserCopy != null) {
        AddCopyUserDialog(
            targetUser = targetAddUserCopy!!,
            onDismiss = { openAddUserCopyDialog = false },
            onAddUser = { newUser -> userViewModel.saveUser(newUser) }
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
            },
            onConfirm = { updated ->
                userViewModel.upsertUser(updated)
            }
        )
    }
}
