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
import com.shary.app.core.dependencyContainer.DependencyContainer
import com.shary.app.services.user.UserService
import com.shary.app.ui.screens.ui_utils.FilterBox
import com.shary.app.ui.screens.ui_utils.GoBackButton
import com.shary.app.ui.screens.ui_utils.SelectableRow
import com.shary.app.utils.DateUtils
import com.shary.app.viewmodels.ViewModelFactory
import com.shary.app.viewmodels.user.UserViewModel
import kotlinx.coroutines.launch
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersScreen(navController: NavHostController, userService: UserService) {

    // Create the ViewModel
    val viewModel: UserViewModel = viewModel(
        factory = ViewModelFactory {
            UserViewModel(
                DependencyContainer.get("user_repository")
            )
        }
    )

    // +++++ Table and DB rows +++++
    val userList by viewModel.users.collectAsState()

    // +++++ Host +++++
    val snackbarHostState = remember { SnackbarHostState() }

    // +++++ Add dialog +++++
    var openAddDialog by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    // +++++ Checked rows +++++
    //val selectedEmails = remember { mutableStateListOf<String>() }
    val selectedEmails by viewModel.selectedEmails.collectAsState()
    // +++++ Search Users +++++
    // 👇 Search state
    var searchText by remember { mutableStateOf("") }
    var searchByEmail by remember { mutableStateOf(true) } // true = search by Email, false = search by Username
    // 👇 Filtered list dynamically
    //val filteredUsers = userList.filter { it.email.contains(searchText, ignoreCase = true) }
    // 👇 Filtered list dynamically based on toggle
    val filteredUsers = userList.filter { user ->
        if (searchByEmail) {
            user.email.contains(searchText, ignoreCase = true)
        } else {
            user.username.contains(searchText, ignoreCase = true)
        }
    }

    val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)

    DisposableEffect(lifecycleOwner.value) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                println("Saving selected emails on stop: $selectedEmails")
                userService.cacheSelectedEmails(selectedEmails)
            }
        }

        val lifecycle = lifecycleOwner.value.lifecycle
        lifecycle.addObserver(observer)

        onDispose {
            lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Users") },
                modifier = Modifier.fillMaxHeight(0.1f),
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    ),
                )
                 },
        floatingActionButton =
        {
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
            )
            {

                // Go back button
                GoBackButton(navController)

                // Add row button
                FloatingActionButton(onClick = { openAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add User")
                }

                // Delete row button
                val isEnabled = selectedEmails.isNotEmpty()
                FloatingActionButton(
                    onClick = {
                        if (isEnabled) {
                            // Copy of selectedUsers
                            selectedEmails.toList().forEach { email ->
                                viewModel.viewModelScope.launch {
                                    val success = viewModel.deleteUser(email)
                                }
                                viewModel.clearSelectedEmails()
                            }
                            snackbarMessage = "Deleted selected users"
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
                .padding(16.dp)
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            HorizontalDivider(thickness = 1.dp, color = Color.Gray)
            Spacer(modifier = Modifier.height(32.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {

                // 🔍 Search and Toggle Filter
                TextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    label = { Text(if (searchByEmail) "Search by Email" else "Search by Username") },
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .padding(vertical = 8.dp, horizontal = 8.dp),
                    singleLine = true
                )

                // Email Box
                FilterBox(
                    title = "Email",
                    isSelected = searchByEmail,
                    onClick = { searchByEmail = true }
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Username Box
                FilterBox(
                    title = "Username",
                    isSelected = !searchByEmail,
                    onClick = { searchByEmail = false }
                )
            }
            Spacer(modifier = Modifier.width(64.dp))

            // 📋 Table header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {

                Spacer(modifier = Modifier.width(24.dp)) // For checkbox spacing
                Text("Email", Modifier.weight(1f))
                Text("Username", Modifier.weight(1f))
                Text("Date Added", Modifier.weight(1f))
            }
            HorizontalDivider(thickness = 1.dp, color = Color.Gray)

            //if (userList.isNotEmpty()) {
            if (filteredUsers.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    //itemsIndexed(userList) { index, user ->
                    itemsIndexed(filteredUsers) { index, user ->
                        SelectableRow(
                            item = user,
                            index = index,
                            isSelected = selectedEmails.contains(user.email),
                            onCheckedChange = { checked ->
                                viewModel.toggleUserSelection(user.email, checked)
                                              },
                        ) { userItem ->
                            Text(userItem.email, Modifier.weight(1f))
                            Text(userItem.username, Modifier.weight(1f))
                            val formattedDate = DateUtils.formatTimeMillis(userItem.dateAdded )
                            Text(formattedDate, Modifier.weight(1f))
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
            //Spacer(modifier = Modifier.height(32.dp))
        }

        if (openAddDialog) {
            AddUserDialog(
                onDismiss = { openAddDialog = false },
                onAddUser = { username, email ->
                    if (username.isNotBlank() && email.isNotBlank()) {
                        val user = User.newBuilder()
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
                        snackbarMessage = "Both Username and Email are required"
                    }
                }
            )
        }
    }
}
