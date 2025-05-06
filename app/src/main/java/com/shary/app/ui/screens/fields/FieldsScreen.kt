package com.shary.app.ui.screens.fields

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import com.shary.app.Field
import com.shary.app.core.dependencyContainer.DependencyContainer
import com.shary.app.services.field.FieldService
import com.shary.app.ui.screens.ui_utils.FilterBox
import com.shary.app.ui.screens.ui_utils.GoBackButton
import com.shary.app.utils.DateUtils
import com.shary.app.viewmodels.ViewModelFactory
import com.shary.app.viewmodels.field.FieldViewModel
import kotlinx.coroutines.launch
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FieldsScreen(navController: NavHostController, fieldService: FieldService) {

    val viewModel: FieldViewModel = viewModel(
        factory = ViewModelFactory {
            FieldViewModel(
                DependencyContainer.get("field_repository")
            )
        }
    )

    val fieldList by viewModel.fields.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var openAddDialog by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    var editingField by remember { mutableStateOf<Field?>(null) }
    var editedValue by remember { mutableStateOf("") }

    val selectedKeys = remember { mutableStateListOf<String>() }

    var searchText by remember { mutableStateOf("") }
    var searchByKey by remember { mutableStateOf(true) }
    val filteredFields = fieldList.filter { field ->
        if (searchByKey)
            field.key.contains(searchText, ignoreCase = true)
        else
            field.keyAlias.contains(searchText, ignoreCase = true)
    }
        .toMutableList()

    fun clearStates() {
        editingField = null
        editedValue = ""
        selectedKeys.clear() // = emptyList<String>() as SnapshotStateList<String>
        searchText = ""
        filteredFields.clear()
    }

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
                val currentFilteredFields = fieldList.filter {
                    if (searchByKey) it.key.contains(searchText, ignoreCase = true)
                    else it.keyAlias.contains(searchText, ignoreCase = true)
                }.filter { it.key in selectedKeys }

                fieldService.cacheSelectedFields(currentFilteredFields)
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
                title = { Text("Fields") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
            )
        },
        floatingActionButton = {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
            ) {
                GoBackButton(navController)

                FloatingActionButton(onClick = { openAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Field")
                }

                val isEnabled = selectedKeys.isNotEmpty()
                FloatingActionButton(
                    onClick = {
                        if (isEnabled) {
                            selectedKeys.toList().forEach { key ->
                                viewModel.viewModelScope.launch {
                                    viewModel.deleteField(key)
                                }
                                selectedKeys.clear()
                            }
                            snackbarMessage = "Deleted selected fields"
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
            HorizontalDivider(thickness = 1.dp, color = Color.Gray)
            Spacer(modifier = Modifier.height(32.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    label = { Text(if (searchByKey) "Search by key" else "Search by alias") },
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .padding(vertical = 8.dp, horizontal = 8.dp),
                    singleLine = true
                )

                FilterBox(
                    "Key",
                    isSelected = searchByKey,
                    onClick = { searchByKey = true },
                )

                Spacer(modifier = Modifier.width(8.dp))

                FilterBox(
                    "Alias",
                    isSelected = !searchByKey,
                    onClick = { searchByKey = false }
                )
            }
            Spacer(modifier = Modifier.width(64.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Key", Modifier.weight(1f))
                Text("Alias", Modifier.weight(1f))
            }
            HorizontalDivider(thickness = 1.dp, color = Color.Gray)

            if (filteredFields.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredFields) { field ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (selectedKeys.contains(field.key))
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    else MaterialTheme.colorScheme.surface
                                )
                                .clickable { if (!selectedKeys.contains(field.key)) selectedKeys.add(field.key) else selectedKeys.remove(field.key) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(field.key, style = MaterialTheme.typography.bodyLarge)
                                Text(field.keyAlias, style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = {
                                editingField = field
                                editedValue = field.value
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No fields available", style = MaterialTheme.typography.bodyMedium)
                }
            }
            HorizontalDivider(thickness = 1.dp, color = Color.Gray)
        }

        if (openAddDialog) {
            AddFieldDialog(
                onDismiss = { openAddDialog = false },
                onAddField = { key, keyAlias, value ->
                    if (key.isNotBlank() && value.isNotBlank()) {
                        val field = Field.newBuilder()
                            .setKey(key)
                            .setKeyAlias(keyAlias)
                            .setValue(value)
                            .setDateAdded(Instant.now().toEpochMilli())
                            .build()

                        viewModel.viewModelScope.launch {
                            val success = viewModel.saveField(field).await()
                            openAddDialog = !success
                            val alterMessage = if (success) "added" else "already exists"
                            snackbarMessage = "Field '$key' $alterMessage"
                        }
                    } else {
                        snackbarMessage = "Key and value are required"
                    }
                }
            )
        }

        editingField?.let { field ->
            AlertDialog(
                onDismissRequest = { editingField = null },
                title = { Text("Update ${field.key}") },
                text = {
                    OutlinedTextField(
                        value = editedValue,
                        onValueChange = { editedValue = it },
                        label = { Text("New Value") },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        //fieldService.updateFieldValue(field, editedValue)
                        viewModel.viewModelScope.launch {
                            viewModel.updateFieldValue(field.key, editedValue)
                        }
                        editingField = null
                        snackbarMessage = "Field '${field.key}' updated"
                    }) { Text("Accept") }
                },
                dismissButton = {
                    TextButton(onClick = { editingField = null }) { Text("Cancel") }
                }
            )
        }
    }
}