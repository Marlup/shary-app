package com.shary.app.ui.screens.fileVisualizer

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.navigation.NavHostController
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.types.enums.AddFlow
import com.shary.app.core.domain.types.enums.DataFileMode
import com.shary.app.core.domain.types.valueobjects.ParsedJson
import com.shary.app.ui.screens.utils.SpecialComponents.CompactActionButton
import com.shary.app.ui.screens.home.utils.Screen
import com.shary.app.ui.screens.utils.FieldMatchingDialog
import com.shary.app.viewmodels.field.FieldViewModel
import com.shary.app.viewmodels.fileVisualizer.FileVisualizerViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileVisualizerScreen(navController: NavHostController) {

    // ---------------- ViewModels ----------------
    val fieldViewModel: FieldViewModel = hiltViewModel()
    val fileVisualizerViewModel: FileVisualizerViewModel = hiltViewModel()

    val items by fileVisualizerViewModel.items.collectAsState()
    val isLoading by fileVisualizerViewModel.isLoading.collectAsState()

    var selected by remember { mutableStateOf<ParsedJson?>(null) }
    var isMatchingOpen by remember { mutableStateOf(false) }

    var lastSubmittedKey by remember { mutableStateOf<String?>(null) }
    var lastFlow by remember { mutableStateOf(AddFlow.NONE) }

    // For file picking
    val jsonPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            uri?.let { fileVisualizerViewModel.importJsonFromUri(it) }
        }
    )

    // Auto-refresh when entering
    LaunchedEffect(Unit) { fileVisualizerViewModel.refreshFiles() }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .padding(end = 16.dp),
                horizontalAlignment = Alignment.End
            )
            {
                var expanded by remember { mutableStateOf(false) }

                Box {
                    IconButton(onClick = { expanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = "Menu"
                        )
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Request") },
                            onClick = {
                                expanded = false
                                navController.navigate(Screen.Requests.route)
                            },
                            leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) }
                        )

                        DropdownMenuItem(
                            text = { Text("Fields") },
                            onClick = {
                                expanded = false
                                fieldViewModel.clearSelectedFields()
                                navController.navigate(Screen.Fields.route)
                            },
                            leadingIcon = { Icon(Icons.Filled.TextFields, contentDescription = null) }
                        )
                    }
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
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
                        onClick = { navController.navigate(Screen.Fields.route) },
                        icon = Icons.Default.TextFields,
                        backgroundColor = colorScheme.primary,
                        contentDescription = "To Fields"
                    )
                    CompactActionButton(
                        onClick = { navController.navigate(Screen.FileVisualizer.route) },
                        icon = Icons.Default.KeyboardDoubleArrowLeft,
                        backgroundColor = colorScheme.primary,
                        contentDescription = "Back to Visualization Home"
                    )
                }

                // ---- Center: Add + Users ----
                Row(
                    modifier = Modifier.weight(0.70f),
                    horizontalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompactActionButton(
                        onClick = {
                            jsonPickerLauncher.launch(
                                arrayOf("application/json", "application/zip")
                            )
                                  },
                        icon = Icons.Default.Add,
                        backgroundColor = colorScheme.primary,
                        contentDescription = "\"Import JSON or ZIP\""
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

                    if (selected?.mode == DataFileMode.Request && selected?.fields?.isNotEmpty() == true) {
                        ExtendedFloatingActionButton(
                            onClick = { isMatchingOpen = true },
                            icon = { Icon(Icons.Filled.PlayArrow, contentDescription = "Start Matching") },
                            text = { Text("Start matching") }
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                items.isEmpty() -> {
                    PlaceholderEmpty()
                }

                selected == null -> {
                    // Show list of available files
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(items) { json ->
                            ElevatedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .border(
                                        width = 4.dp,
                                        color = colorScheme.primary,
                                        shape = MaterialTheme.shapes.medium
                                    ),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = Color.White
                                ),
                                onClick = { selected = json }
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(json.fileName, style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.height(4.dp))
                                    Text("Mode: ${json.mode ?: "Unknown"}", style = MaterialTheme.typography.bodySmall)
                                    Text("Valid: ${json.isValidStructure}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                selected?.mode == DataFileMode.Response -> {
                    FieldsList(fields = selected!!.fields)
                }

                selected?.mode == DataFileMode.Request -> {
                    RequestPairsList(
                        pairs = selected!!.fields.map { it.key to it.value }
                    )
                }
            }

            if (isMatchingOpen && selected?.mode == DataFileMode.Request) {
                FieldMatchingDialog(
                    storedFields = selected!!.fields,
                    requestKeys = selected!!.fields.map { it.key to it.value },
                    onDismiss = { isMatchingOpen = false },
                    onAccept = { /* handle accept */ },
                    onAddField = { newField ->
                        lastFlow = AddFlow.ADD
                        lastSubmittedKey = newField.key
                        fieldViewModel.addField(newField)
                    },
                )
            }
        }
    }
}

@Composable
private fun FieldsList(fields: List<FieldDomain>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        items(fields) { f ->
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(f.key, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(f.value, style = MaterialTheme.typography.bodyMedium)
                    //Spacer(Modifier.height(2.dp))
                    //Text("Tag: ${f.tag}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun RequestPairsList(pairs: List<Pair<String, String>>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        items(pairs) { (k, v) ->
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(k, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(v, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun PlaceholderEmpty() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("No files available")
    }
}
