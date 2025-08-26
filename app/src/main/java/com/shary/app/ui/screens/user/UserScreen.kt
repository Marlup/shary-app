package com.shary.app.ui.screens.user

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
import com.shary.app.core.domain.models.UserDomain
import com.shary.app.core.domain.types.enums.AddFlow
import com.shary.app.core.domain.types.enums.UserAttribute
import com.shary.app.ui.screens.user.components.AddCopyUserDialog
import com.shary.app.ui.screens.user.components.AddUserDialog
import com.shary.app.ui.screens.utils.GoBackButton
import com.shary.app.ui.screens.utils.RowSearcher
import com.shary.app.ui.screens.utils.SelectableRow
import com.shary.app.ui.screens.utils.UserItemRow
import com.shary.app.viewmodels.user.UserEvent
import com.shary.app.viewmodels.user.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersScreen(navController: NavHostController) {
    // ---- ViewModel ----
    val userViewModel: UserViewModel = hiltViewModel()

    // ---- State from VM ----
    val userList by userViewModel.users.collectAsState()
    val selectedUsers by userViewModel.selectedUsers.collectAsState()
    val selectedPhoneNumber by userViewModel.selectedPhoneNumber.collectAsState()
    val isLoading by userViewModel.isLoading.collectAsState()

    // ---- UI state ----
    val snackbarHostState = remember { SnackbarHostState() }
    var openAddDialog by remember { mutableStateOf(false) }
    var openAddUserCopyDialog by remember { mutableStateOf(false) }
    var targetAddUserCopy by remember { mutableStateOf<UserDomain?>(null) }
    var editingUser by remember { mutableStateOf<UserDomain?>(null) }
    var editedValue by remember { mutableStateOf("") }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    //  ---- Search Users ----
    var searchCriteria by remember { mutableStateOf("") }
    var userSearchAttribute by remember { mutableStateOf(UserAttribute.Username) }

    val filteredUsers = remember(userList, searchCriteria, userSearchAttribute) {
        userList.filter { filterUsersBy(it, searchCriteria, userSearchAttribute)
        }.toMutableList()
    }

    // ---- Event-driven UX ----
    var lastFlow by remember { mutableStateOf(AddFlow.NONE) }
    var lastSubmittedEmail by remember { mutableStateOf<String?>(null) }

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
            if (event == Lifecycle.Event.ON_STOP) {
                userViewModel.setSelectedUsers(selectedUsers) // <-- persists to Session
                clearEphemeralStates()
            }
        }
        val lifecycle = lifecycleOwner.value.lifecycle
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    // ---- UI ----
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Users") },
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
                GoBackButton(navController)

                FloatingActionButton(
                    onClick = { openAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) { Icon(Icons.Default.Add, contentDescription = "Add User") }

                val canDelete = selectedUsers.isNotEmpty()
                FloatingActionButton(
                    onClick = {
                        if (canDelete) {
                            // delete selected
                            selectedUsers.toList().forEach { selectedUser ->
                                userViewModel.deleteUser(selectedUser)
                            }
                            userViewModel.clearSelectedUsers()
                            snackbarMessage = "Deleted ${selectedUsers.size} user(s)"
                        }
                    },
                    containerColor = if (canDelete) MaterialTheme.colorScheme.primary else Color.Gray,
                    contentColor = if (canDelete) Color.White else Color.LightGray,
                    modifier = Modifier.alpha(if (canDelete) 1f else 0.6f)
                ) { Icon(Icons.Default.Delete, contentDescription = "Delete users") }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(8.dp)
                .fillMaxWidth()
                .fillMaxHeight(0.90f),
            horizontalAlignment = Alignment.Start
        ) {

            // Search header
            RowSearcher(
                searchText = searchCriteria,
                onSearchTextChange = { searchCriteria = it },
                currentAttribute = userSearchAttribute,
                onAttributeChange = { userSearchAttribute = it },
                availableAttributes = UserAttribute.entries,
                resolveOptionText = { userAttribute ->
                    userSearchAttribute = userAttribute
                    userAttribute.name
                }
            )

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            } else if (filteredUsers.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(
                        items = filteredUsers,
                        key = { _, user -> user.email }
                    ) { index, user ->
                        val isSelected = selectedUsers.contains(user)

                        val rowBackgroundColor = when {
                            isSelected -> Color.LightGray // ← selection color
                            index % 2 == 0 -> MaterialTheme.colorScheme.surface                     // alternate / tag color
                            else -> MaterialTheme.colorScheme.secondaryContainer
                        }

                        SelectableRow(
                            item = user,
                            index = index,
                            backgroundColorProvider = { rowBackgroundColor },
                            onToggle = { userViewModel.toggleUser(user) }
                        ) { userItem ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(0.95f)
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                UserItemRow(
                                    user = userItem,
                                    titleColor = rowBackgroundColor,
                                    onEditClick = {
                                        editingUser = userItem
                                        editedValue = userItem.email
                                    },
                                    onAddItemCopyClick = {
                                        openAddUserCopyDialog = true
                                        targetAddUserCopy = userItem
                                    },
                                )
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
}

fun filterUsersBy(candidateUser: UserDomain, criteria: String, userSearchBy: UserAttribute): Boolean {
    val isValidUser = when (userSearchBy) {
        UserAttribute.Username ->
            candidateUser.username.contains(criteria, ignoreCase = true)
        UserAttribute.Email ->
            candidateUser.email.orEmpty().contains(criteria, ignoreCase = true)
    }
    return isValidUser
}
