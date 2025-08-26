package com.shary.app.ui.screens.request

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.shary.app.core.domain.types.enums.UiFieldTag
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.ui.screens.request.utils.AddRequestDialog
import com.shary.app.ui.screens.request.utils.SendRequestDialog
import com.shary.app.ui.screens.utils.GoBackButton
import com.shary.app.ui.screens.utils.SelectableRow
import com.shary.app.viewmodels.request.RequestListViewModel
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestsScreen(navController: NavHostController) {
    val requestViewModel: RequestListViewModel = hiltViewModel()

    val snackbarHostState = remember { SnackbarHostState() }

    // Local working list of requested fields (Domain)
    val requestFields = remember { mutableStateListOf<FieldDomain>() }

    var openAddDialog by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var openSendDialog by remember { mutableStateOf(false) }

    val selectedKeys = remember { mutableStateListOf<String>() }

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
                    requestViewModel.addRequestFromFields(requestFields.toList())
                    requestFields.clear()
                }
            }
        }
        val lifecycle = lifecycleOwner.value.lifecycle
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Requests") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                expandedHeight = 30.dp
            )
        },
        floatingActionButton = {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
            ) {
                // Back
                GoBackButton(navController)

                // Add field to the current request
                FloatingActionButton(onClick = { openAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Request Field")
                }

                // Delete selected fields from the current request
                val isEnabled = selectedKeys.isNotEmpty()
                FloatingActionButton(
                    onClick = {
                        if (isEnabled) {
                            requestFields.removeAll { it.key in selectedKeys }
                            selectedKeys.clear()
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
                        key = { _, field -> field.key } // stable
                    ) { index, field ->
                        val isSelected = field.key in selectedKeys

                        val backgroundColor = when {
                            isSelected -> MaterialTheme.colorScheme.secondaryContainer // highlight selection
                            index % 2 == 0 -> MaterialTheme.colorScheme.surface
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }

                        val rowBackgroundColor = when {
                            isSelected -> Color.LightGray // ← selection color
                            index % 2 == 0 -> MaterialTheme.colorScheme.surface                     // alternate / tag color
                            else -> MaterialTheme.colorScheme.secondaryContainer
                        }

                        SelectableRow(
                            item = field,
                            index = index,
                            backgroundColorProvider = { backgroundColor },
                            onToggle = {
                                if (isSelected) selectedKeys.remove(field.key)
                                else selectedKeys.add(field.key)
                            }
                        ) { fieldItem ->
                            Row(Modifier.fillMaxWidth()) {
                                Text(fieldItem.key, Modifier.weight(1f))
                                Text(fieldItem.keyAlias.orEmpty(), Modifier.weight(1f))
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
                            tag = UiFieldTag.Unknown,
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
                    // TODO: Implement email sending
                }
            )
        }
    }
}
