package com.shary.app.ui.screens.fileVisualizer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.types.enums.AddFlow
import com.shary.app.core.domain.types.enums.DataFileMode
import com.shary.app.core.domain.types.enums.UiFieldTag
import com.shary.app.ui.screens.utils.FieldMatchingDialog
import com.shary.app.ui.screens.utils.GoBackButton
import com.shary.app.viewmodels.field.FieldViewModel
import com.shary.app.viewmodels.fileVisualizer.FileVisualizerViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileVisualizerScreen(navController: NavHostController) {
    val fieldViewModel: FieldViewModel = hiltViewModel()
    val fileVisualizerViewModel: FileVisualizerViewModel = hiltViewModel()

    val items by fileVisualizerViewModel.items.collectAsState()
    val isLoading by fileVisualizerViewModel.isLoading.collectAsState()

    var selected by remember { mutableStateOf<FileVisualizerViewModel.ParsedZip?>(null) }
    var isMatchingOpen by remember { mutableStateOf(false) }

    var lastSubmittedKey by remember { mutableStateOf<String?>(null) }
    var lastFlow by remember { mutableStateOf(AddFlow.NONE) }

    // Auto-refresh when entering
    LaunchedEffect(Unit) { fileVisualizerViewModel.refreshFiles() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("File Visualizer") },
                navigationIcon = {  },
                actions = {
                    IconButton(onClick = { fileVisualizerViewModel.refreshFiles() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
            ) {

                // Go back to home button
                GoBackButton(navController)

                if (selected?.mode == DataFileMode.Request && selected?.fields?.isNotEmpty() == true) {
                    ExtendedFloatingActionButton(
                        onClick = { isMatchingOpen = true },
                        icon = { Icon(Icons.Filled.PlayArrow, contentDescription = "Start Matching") },
                        text = { Text("Start matching") }
                    )
                }
            }
        }
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
                        items(items) { zip ->
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { selected = zip }
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(zip.fileName, style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.height(4.dp))
                                    Text("Mode: ${zip.mode ?: "Unknown"}", style = MaterialTheme.typography.bodySmall)
                                    Text("Valid: ${zip.isValidStructure}", style = MaterialTheme.typography.bodySmall)
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
                    availableTags = UiFieldTag.entries
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
