package com.shary.app.ui.screens.user

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
//import androidx.hilt.navigation.compose.hiltViewModel // deprecated location of hiltViewModel
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.shary.app.core.domain.models.UserDomain
import com.shary.app.core.domain.types.enums.AddFlow
import com.shary.app.core.domain.types.enums.UserAttribute
import com.shary.app.ui.screens.utils.SpecialComponents.CompactActionButton
import com.shary.app.ui.screens.home.utils.Screen
import com.shary.app.ui.screens.user.components.AddCopyUserDialog
import com.shary.app.ui.screens.user.components.AddUserDialog
import com.shary.app.ui.screens.utils.RowSearcher
import com.shary.app.viewmodels.field.FieldViewModel
import com.shary.app.viewmodels.user.UserEvent
import com.shary.app.viewmodels.user.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersScreen(navController: NavHostController) {
    // ---- ViewModel ----
    val userViewModel: UserViewModel = hiltViewModel()
    val fieldViewModel: FieldViewModel = hiltViewModel()

    // ---- State from VM ----
    val userList by userViewModel.users.collectAsState()
    val selectedUsers by userViewModel.selectedUsers.collectAsState()
    val isLoading by userViewModel.isLoading.collectAsState()

    // ---- UI state ----
    val snackbarHostState = remember { SnackbarHostState() }
    var openAddDialog by remember { mutableStateOf(false) }
    var openAddUserCopyDialog by remember { mutableStateOf(false) }
    var targetAddUserCopy by remember { mutableStateOf<UserDomain?>(null) }
    var editingUser by remember { mutableStateOf<UserDomain?>(null) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    //  ---- Search Users ----
    var searchCriteria by remember { mutableStateOf("") }
    var userSearchAttribute by remember { mutableStateOf(UserAttribute.Username) }

    val filteredUsers = remember(userList, searchCriteria, userSearchAttribute) {
        userList.filter { it.matchBy(searchCriteria, userSearchAttribute) }.toMutableList()
    }

    // ---- Event-driven UX ----
    var lastFlow by remember { mutableStateOf(AddFlow.NONE) }
    val lastSubmittedEmail by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        userViewModel.events.collect { ev ->
            when (ev) {
                is UserEvent.Saved -> {
                    if (lastFlow == AddFlow.COPY) {
                        openAddUserCopyDialog = false
                        snackbarMessage = "Copied user '${lastSubmittedEmail.orEmpty()}' added"
                    } else {
                        openAddDialog = false
                        snackbarMessage = "User '${lastSubmittedEmail.orEmpty()}' added"
                    }
                    lastFlow = AddFlow.NONE
                }
                is UserEvent.AlreadyExists -> {
                    val label = if (lastFlow == AddFlow.COPY) "Copied user" else "User"
                    snackbarMessage = "$label '${lastSubmittedEmail.orEmpty()}' already exists"
                    lastFlow = AddFlow.NONE
                }
                is UserEvent.Deleted -> {
                    snackbarMessage = "Deleted '${ev}'"
                }
                is UserEvent.Error -> {
                    snackbarMessage = "Error: ${ev.throwable.message}"
                    lastFlow = AddFlow.NONE
                }
            }
        }
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    // ---- Lifecycle: persist selection into Session via VM ----
    fun clearEphemeralStates() {
        searchCriteria = ""
    }
    val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)
    DisposableEffect(lifecycleOwner.value) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP)
            {
                userViewModel.cacheUsers(selectedUsers)
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
            Column(
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 64.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    //GoToScreen(navController, Screen.Fields) { userViewModel.clearSelectedUsers() }

                }
                RowSearcher(
                    queryText = searchCriteria,
                    onQueryTextChange = { searchCriteria = it },
                    currentAttribute = userSearchAttribute,
                    onAttributeChange = { userSearchAttribute = it },
                    availableAttributes = UserAttribute.entries,
                    resolveOptionText = { userSearchAttribute = it; it.name }
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
                // ---- Left: Delete ----
                Box(
                    modifier = Modifier
                        .weight(0.15f),
                    contentAlignment = Alignment.Center
                ) {
                    CompactActionButton(
                        onClick = {
                            if (selectedUsers.isNotEmpty()) {
                                userViewModel.deleteUsers(selectedUsers)
                                userViewModel.clearSelectedUsers()
                                snackbarMessage = "Deleted ${selectedUsers.size} fields"
                            }
                        },
                        backgroundColor = colorScheme.error,
                        icon = Icons.Default.Delete,
                        contentDescription = "Delete Users",
                        enabled = selectedUsers.isNotEmpty()
                    )
                }

                // ---- Center: Add + Fields ----
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
                        onClick = { navController.navigate(Screen.Fields.route) },
                        icon = Icons.Default.TextFields,
                        backgroundColor = colorScheme.primary,
                        contentDescription = "Send to Users"
                    )
                }

                // ---- Right: Summary ----
                Box(
                    modifier = Modifier
                        .weight(0.15f),
                    contentAlignment = Alignment.Center
                ) {
                    CompactActionButton(
                        onClick = {
                            if (fieldViewModel.anyFieldCached() && selectedUsers.isNotEmpty()) {
                                navController.navigate(Screen.Summary.route)
                            }
                        },
                        icon = Icons.Default.AssignmentTurnedIn,
                        backgroundColor = colorScheme.tertiary,
                        contentDescription = "Summary",
                        enabled = selectedUsers.isNotEmpty() && fieldViewModel.anyFieldCached()
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
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            horizontalAlignment = Alignment.Start
        ) {

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            } else if (filteredUsers.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .padding(start=16.dp, end=16.dp),
                ) {
                    itemsIndexed(
                        items = filteredUsers,
                        key = { _, user -> user.email }
                    ) { index, user ->

                        val isSelected = selectedUsers.any { it == user } // comparar por key

                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .border(
                                    width = if (isSelected) 6.dp else 4.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = MaterialTheme.shapes.medium
                                ),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = Color.White //field.tag.safeColor().copy(alpha = 1.0f)
                            ),
                            onClick = { userViewModel.toggleUser(user) }
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
                                        user.username,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1
                                    )
                                    // value text
                                    Text(
                                        user.email,
                                        style = MaterialTheme.typography.bodyMedium,
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
                                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy User Content")
                                                Text("Copy")
                                                   },
                                            onClick = {
                                                expanded = false
                                                // Direct copy to clipboard or VM
                                                snackbarMessage = "User '${user.username}' copied"
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = {
                                                Icon(Icons.Default.CopyAll, contentDescription = "Add Copy from User Content")
                                                Text("Add Copy")
                                                   },
                                            onClick = {
                                                expanded = false
                                                openAddUserCopyDialog = true
                                                targetAddUserCopy = user
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit User Content")
                                                Text("Edit")
                                                   },
                                            onClick = {
                                                expanded = false
                                                editingUser = user
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) { Text("No users available", style = MaterialTheme.typography.bodyMedium) }
            }
        }
        HorizontalDivider(thickness = 1.dp, color = Color.Gray)
    }

    // ---- Add User Dialog ----
    if (openAddDialog) {
        AddUserDialog(
            onDismiss = { openAddDialog = false },
            onAddUser = { newUser ->
                userViewModel.saveUser(newUser) // el VM orquesta corutinas, eventos y refresh
            }
        )
    }

    // ---- Add Copy User Dialog ----
    if (openAddUserCopyDialog && targetAddUserCopy != null) {
        AddCopyUserDialog(
            targetUser = targetAddUserCopy!!,
            onDismiss = { openAddUserCopyDialog = false },
            onAddUser = { newUser ->
                userViewModel.saveUser(newUser)
            }
        )
    }
}

