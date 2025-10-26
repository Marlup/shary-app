package com.shary.app.ui.screens.request

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.shary.app.core.domain.types.enums.Tag
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.ui.screens.utils.SpecialComponents.CompactActionButton
import com.shary.app.ui.screens.home.utils.Screen
import com.shary.app.ui.screens.request.utils.AddRequestDialog
import com.shary.app.ui.screens.request.utils.SendRequestDialog
import com.shary.app.viewmodels.communication.CloudViewModel
import com.shary.app.viewmodels.communication.EmailViewModel
import com.shary.app.viewmodels.field.FieldViewModel
import com.shary.app.viewmodels.user.UserViewModel
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestsScreen(navController: NavHostController) {

    // ---------------- ViewModels ----------------
    val fieldViewModel: FieldViewModel = hiltViewModel()
    val userViewModel: UserViewModel = hiltViewModel()
    val emailViewModel: EmailViewModel = hiltViewModel()
    val cloudViewModel: CloudViewModel = hiltViewModel()

    val snackbarHostState = remember { SnackbarHostState() }

    // Local working list of requested fields (Domain)
    val requestFields = remember { mutableStateListOf<FieldDomain>() }

    var openAddDialog by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var openSendDialog by remember { mutableStateOf(false) }

    // ---- Checked rows (Domain) ----
    val selectedFields by fieldViewModel.selectedFields.collectAsState()

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)

    DisposableEffect(lifecycleOwner.value) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                // Persist a Request built from the current local list
                if (requestFields.isNotEmpty()) {
                    TODO()
                }
            }
        }
        val lifecycle = lifecycleOwner.value.lifecycle
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    // ---------------- UI ----------------
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Requests") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                expandedHeight = 64.dp
            )
        },
        floatingActionButton = {

            HorizontalDivider(thickness = 1.dp)

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

                // ---- Center: Add + Users ----
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
                        onClick = { navController.navigate(Screen.Users.route) },
                        icon = Icons.Default.Person,
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxWidth()
                .fillMaxHeight(0.90f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    "Key",
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Key Alias",
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider(thickness = 1.dp, color = Color.Gray)

            if (requestFields.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(
                        items = requestFields,
                        key = { _, field -> field.key } // stable key
                    ) { index, field ->
                        val isSelected = field in selectedFields

                        // background colors
                        val backgroundColor = when {
                            isSelected -> colorScheme.secondaryContainer
                            index % 2 == 0 -> colorScheme.surface
                            else -> colorScheme.surfaceVariant
                        }

                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .alpha(if (isSelected) 1f else 0.9f),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = backgroundColor
                            ),
                            onClick = {
                                if (selectedFields.isNotEmpty()) {
                                    fieldViewModel.deleteFields(selectedFields)
                                    fieldViewModel.clearSelectedFields()
                                    snackbarMessage = "Deleted ${selectedFields.size} fields"
                                }
                            },
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    field.key,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    field.keyAlias.orEmpty(),
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
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
                    Text("No requests available", style = MaterialTheme.typography.bodyMedium)
                }
            }
            HorizontalDivider(thickness = 1.dp, color = Color.Gray)
        }

        // Add field (for the in-progress request)
        if (openAddDialog) {
            AddRequestDialog(
                onDismiss = { openAddDialog = false },
                onAddRequest = { key, keyAlias ->
                    if (key.isNotBlank()) {
                        val field = FieldDomain(
                            key = key.trim(),
                            keyAlias = keyAlias.trim().ifBlank { null },
                            value = "", // requests only need the key; keep value empty
                            tag = Tag.Unknown,
                            dateAdded = Instant.now()
                        )
                        requestFields.add(field)
                        openAddDialog = false
                        snackbarMessage = "Requested key '$key' added"
                    } else {
                        snackbarMessage = "Requested key is required"
                    }
                }
            )
        }

        // Send (placeholder)
        if (openSendDialog) {
            SendRequestDialog(
                onDismiss = { openSendDialog = false },
                onSend = {
                    openSendDialog = false
                    userViewModel.getOwnerEmail()?.let {
                        cloudViewModel.uploadData(
                            selectedFields,
                            it,
                            userViewModel.getCachedUsers(),
                            true
                        )
                    }
                }
            )
        }
    }
}
