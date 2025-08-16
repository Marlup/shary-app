package com.shary.app.ui.screens.fileVisualizer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.shary.app.core.domain.types.enums.AddFlow
import com.shary.app.core.domain.types.enums.UiFieldTag
import com.shary.app.ui.screens.utils.FieldMatchingDialog
import com.shary.app.ui.screens.utils.GoBackButton
import com.shary.app.viewmodels.field.FieldViewModel
import com.shary.app.viewmodels.fileVisualizer.FileVisualizerViewModel
import com.shary.app.viewmodels.tags.TagViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileVisualizerScreen (navController: NavHostController) {

    val fieldViewModel: FieldViewModel = hiltViewModel() // para cargar/guardar campos locales
    val fileVisualizerViewModel: FileVisualizerViewModel = hiltViewModel()
    //val tagViewModel: TagViewModel = hiltViewModel()

    val items by fileVisualizerViewModel.items.collectAsState()
    val isLoading by fileVisualizerViewModel.isLoading.collectAsState()

    // We’ll take the first selected/only item for now
    val selected = items.firstOrNull()
    var isMatchingOpen by remember { mutableStateOf(false) }

    // ---- Field Matching and Add dialogs ----
    var lastSubmittedKey by remember { mutableStateOf<String?>(null) }
    var lastFlow by remember { mutableStateOf(AddFlow.NONE) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("File Visualizer") },
                navigationIcon = { GoBackButton(navController) } // Back button
            )
        },
        floatingActionButton = {
            if (selected?.mode.equals("request", ignoreCase = true) &&
                (selected?.fields?.isNotEmpty() == true)
            ) {
                ExtendedFloatingActionButton(
                    onClick = { isMatchingOpen = true },
                    icon = { Icon(Icons.Filled.PlayArrow, contentDescription = "Start Matching") },
                    text = { Text("Start matching") }
                )
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

                selected == null -> {
                    PlaceholderEmpty()
                }

                selected.mode.equals("response", ignoreCase = true) -> {
                    FieldsList(fields = selected.fields)
                }

                selected.mode.equals("request", ignoreCase = true) -> {
                    RequestPairsList(
                        pairs = selected.fields.map { it.key to it.value }
                    )
                }

                else -> {
                    PlaceholderEmpty()
                }
            }

            if (isMatchingOpen && selected?.mode.equals("request", ignoreCase = true)) {
                // Map FieldDomain -> proto Field for MatchingDialog
                val storedFields = selected?.fields

                val requestKeys = selected?.fields?.map { it.key to it.value }

                FieldMatchingDialog(
                    storedFields = storedFields!!,
                    requestKeys = requestKeys!!,
                    onDismiss = { isMatchingOpen = false },
                    onAccept = { /* handle accept */ },
                    onAddField = { newField ->
                        lastFlow = AddFlow.ADD
                        lastSubmittedKey = newField.key

                        fieldViewModel.addField(newField) // VM orchestrates: custom tag + save + events + refresh
                    },
                    availableTags = UiFieldTag.entries
                )
            }
        }
    }
}

@Composable
private fun FieldsList(fields: List<com.shary.app.core.domain.models.FieldDomain>) {
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
                    Spacer(Modifier.height(2.dp))
                    Text("Tag: ${f.tag}", style = MaterialTheme.typography.bodySmall)
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
        Text("No file loaded")
    }
}
