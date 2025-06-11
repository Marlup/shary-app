package com.shary.app.ui.screens.users

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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.shary.app.User
import com.shary.app.core.Session
import com.shary.app.services.user.UserService
import com.shary.app.ui.screens.utils.GoBackButton
import com.shary.app.ui.screens.utils.ItemRow
import com.shary.app.ui.screens.utils.RowSearcher
import com.shary.app.ui.screens.utils.SelectableRow
import com.shary.app.viewmodels.ViewModelFactory
import com.shary.app.viewmodels.user.UserViewModel
import kotlinx.coroutines.launch
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersScreen(
    navController: NavHostController,
    session: Session,
    userViewModelFactory: ViewModelFactory<UserViewModel>,
    userService: UserService
) {

    // ---- Create the ViewModel ----
    val viewModel: UserViewModel = viewModel(factory = userViewModelFactory)

    // ---- Table and DB rows ----
    val userList by viewModel.users.collectAsState()

    // ---- Host ----
    val snackbarHostState = remember { SnackbarHostState() }

    // ---- Add dialog ----
    var openAddDialog by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    // ---- Editing/Updating field ----
    var editingUser by remember { mutableStateOf<User?>(null) }
    var editedValue by remember { mutableStateOf("") }

    // ---- Checked rows ----
    val selectedEmails by viewModel.selectedEmails.collectAsState()
    val selectedPhoneNumber by viewModel.selectedPhoneNumber.collectAsState()

    // ---- Search Users ----
    // 👇 Search state
    var searchText by remember { mutableStateOf("") }
    var searchByEmail by remember { mutableStateOf(true) } // true = search by Email, false = search by Username

    // 👇 Filtered list dynamically
    //val filteredUsers = userList.filter { it.email.contains(searchText, ignoreCase = true) }

    // 👇 Filtered list dynamically based on toggle
    val filteredUsers = userList.filter { user ->
        if (searchByEmail)
            user.email.contains(searchText, ignoreCase = true)
        else
            user.username.contains(searchText, ignoreCase = true)
    }
        .toMutableList()

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    fun clearStates() {
        searchText = ""
        filteredUsers.clear()
    }

    val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)

    DisposableEffect(lifecycleOwner.value) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                println("Saving selected emails on stop: $selectedEmails")

                session.cacheSelectedEmails(selectedEmails)
                session.cacheSelectedPhoneNumbers(selectedPhoneNumber)
                // Drop remembered screen states
                clearStates()
            }
        }

        val lifecycle = lifecycleOwner.value.lifecycle
        lifecycle.addObserver(observer)

        onDispose {
            lifecycle.removeObserver(observer)
        }
    }

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
                modifier = Modifier
                    .padding(end = 8.dp, bottom = 8.dp)
            ) {

                // Go back button
                GoBackButton(navController)

                // Add row button
                FloatingActionButton(onClick = { openAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Field")
                }

                // Delete row button
                val isEnabled = selectedEmails.isNotEmpty()
                FloatingActionButton(
                    onClick = {
                        if (isEnabled) {
                            // Copy of selectedUsers
                            selectedEmails.toList().forEach { email ->
                                viewModel.viewModelScope.launch {
                                    viewModel.deleteUser(email)
                                }
                                viewModel.clearSelectedEmails()
                            }
                            snackbarMessage = "Deleted users"
                        }
                    },
                    containerColor = if (isEnabled) MaterialTheme.colorScheme.primary else Color.Gray,
                    contentColor = if (isEnabled) Color.White else Color.LightGray,
                    modifier = Modifier.alpha(if (isEnabled) 1f else 0.6f)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete users")
                }
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
            horizontalAlignment =  Alignment.Start, //Alignment.CenterHorizontally
        ) {
            // Right: Search input
            RowSearcher(
                searchText,
                onSearchTextChange = { searchText = it },
                searchByFirstColumn = searchByEmail,
                onSearchByChange = { searchByEmail = it },
                Pair("username", "email")
            )

            if (filteredUsers.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(filteredUsers) { index, user ->
                        SelectableRow(
                            item = user,
                            index = index,
                            isSelected = selectedEmails.contains(user.email),
                            onCheckedChange = { checked ->
                                viewModel.toggleUserSelection(user.email, checked)
                            },
                        ) { _ ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(0.95f)
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Column with key + value
                                ItemRow(
                                    onEditClick = {
                                        editingUser = userService.valuesToUser(
                                            user.username,
                                            user.email
                                        )
                                        editedValue = user.email },
                                    getTitle = { user.username },
                                    getSubtitle = { "- ${user.email}" },
                                    getTooltip = { "" },
                                    getCopyToClipboard = { "${user.username}: ${user.email}" }
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
                ) {
                    Text("No users available", style = MaterialTheme.typography.bodyMedium)
                }
            }
            HorizontalDivider(thickness = 1.dp, color = Color.Gray)
        }

        if (openAddDialog) {
            AddUserDialog(
                onDismiss = { openAddDialog = false },
                onAddUser = { username, email ->
                    if (username.isNotBlank() && email.isNotBlank()) {
                        val user = User
                            .newBuilder()
                            .setUsername(username)
                            .setEmail(email)
                            .setDateAdded(Instant.now().toEpochMilli()) // simple date example
                            .build()

                        viewModel.viewModelScope.launch {
                            val success = viewModel.saveUser(user).await()
                            // Close dialog if successful user added
                            openAddDialog = !success
                            val alterMessage = if (success) "added" else "already exists"
                            snackbarMessage = "Email '$email' $alterMessage"
                        }
                    } else {
                        snackbarMessage = "Username and email are required"
                    }
                }
            )
        }
    }
}
