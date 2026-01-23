package com.shary.app.ui.screens.utils

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.types.enums.safeColor
import com.shary.app.core.domain.types.valueobjects.MatchingState
import com.shary.app.ui.screens.field.components.AddFieldDialog
import com.shary.app.utils.MatchingHistoryController

@Composable
fun FieldMatchingDialog(
    storedFields: List<FieldDomain>,
    requestFields: List<FieldDomain>,
    onDismiss: () -> Unit,
    onAccept: (List<FieldDomain>) -> Unit,
    onAddField: (FieldDomain) -> Unit,
) {
    val localStoredFields = remember { mutableStateListOf<FieldDomain>() }
    var selectedStorageIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedRequestIndex by rememberSaveable { mutableStateOf<Int?>(null) }

    // Triple<storageIdx, requestIdx, matchId>
    val matches = remember { mutableStateListOf<Triple<Int, Int, Int>>() }
    val freeLabelsFromUnmatched = remember { mutableStateListOf<Int>() }

    var isStorageFirst by rememberSaveable { mutableStateOf(false) }
    var openAddDialog by remember { mutableStateOf(false) }

    // --- Undo/Redo containers ---
    val past = remember { mutableStateListOf<MatchingState>() }
    val future = remember { mutableStateListOf<MatchingState>() }

    // --- Snapshot current state ---
    fun snapshot(): MatchingState = MatchingState(
        selectedStorageIndex = selectedStorageIndex,
        selectedRequestIndex = selectedRequestIndex,
        isStorageFirst = isStorageFirst,
        matches = matches.toList(),
        freeLabelsFromUnmatched = freeLabelsFromUnmatched.toList(),
    )

    // --- Snapshot current matching-related state ---
    fun snapshotState(): MatchingState = MatchingState(
        selectedStorageIndex = selectedStorageIndex,
        selectedRequestIndex = selectedRequestIndex,
        isStorageFirst = isStorageFirst,
        matches = matches.toList(),
        freeLabelsFromUnmatched = freeLabelsFromUnmatched.toList(),
    )

    // --- Restore a previously stored matching state ---
    fun restoreState(state: MatchingState) {
        selectedStorageIndex = state.selectedStorageIndex
        selectedRequestIndex = state.selectedRequestIndex
        isStorageFirst = state.isStorageFirst

        matches.clear()
        matches.addAll(state.matches)

        freeLabelsFromUnmatched.clear()
        freeLabelsFromUnmatched.addAll(state.freeLabelsFromUnmatched)
    }

    // --- Controller ---
    val history = remember {
        MatchingHistoryController(
            snapshot = ::snapshotState,
            restore = ::restoreState
        )
    }


    LaunchedEffect(storedFields, requestFields) {
        history.clear()
        localStoredFields.clear()
        localStoredFields.addAll(storedFields)
    }

    fun clearPendingSelections() {
        selectedStorageIndex = null
        selectedRequestIndex = null
    }

    fun matchIfPossible() {
        FunctionUtils.matchIfPossible(
            storageIdx = selectedStorageIndex,
            requestIdx = selectedRequestIndex,
            matches = matches,
            freeLabelsFromUnmatched = freeLabelsFromUnmatched,
            isStorageFirst = isStorageFirst,
            onUnselect = { clearPendingSelections() },
            onIsStorageFirst = { isStorageFirst = it },
            onFreeLabelStored = { freeLabelsFromUnmatched.add(it) }
        )
    }

    fun buildAcceptedSelection(): List<FieldDomain> =
        matches.sortedBy { it.second }
            .mapNotNull { (sIdx, _, _) -> localStoredFields.getOrNull(sIdx) }

    val isFullyMatched = matches.size == requestFields.size
    val matchedCount = matches.size
    val totalToMatch = requestFields.size.coerceAtLeast(0)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 3.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .heightIn(min = 240.dp, max = 620.dp)
            ) {
                // Actions row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        enabled = isFullyMatched,
                        onClick = {
                            onAccept(buildAcceptedSelection())
                            onDismiss()
                        }
                    ) { Text("Accept") }
                    Spacer(Modifier.width(12.dp))
                    SmallFloatingActionButton(onClick = { openAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Field")
                    }

                    Spacer(Modifier.width(8.dp))
                }

                Row {

                }
                IconButton(
                    onClick = { history.undo() },
                    enabled = history.canUndo
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }

                IconButton(
                    onClick = { history.redo() },
                    enabled = history.canRedo
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward")
                }

                Spacer(Modifier.height(10.dp))

                // Status row (simple, optional but useful)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AssistChip(
                        onClick = { /* no-op */ },
                        label = { Text("Matched: $matchedCount/$totalToMatch") }
                    )
                    if (totalToMatch > 0) {
                        val progress = matchedCount.toFloat() / totalToMatch.toFloat()
                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .width(160.dp)
                                .height(8.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Content
                Row(modifier = Modifier.fillMaxWidth()) {

                    // LEFT: Stored
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Stored",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )

                        if (localStoredFields.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 180.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No stored fields", style = MaterialTheme.typography.bodyMedium)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 460.dp),
                                contentPadding = PaddingValues(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(
                                    items = localStoredFields,
                                    key = { index, field -> "${field.key}#$index" }
                                ) { index, field ->
                                    val isSelected = selectedStorageIndex == index
                                    val match = matches.firstOrNull { it.first == index }
                                    val isMatched = match != null

                                    val backgroundColor = when {
                                        isSelected -> field.tag.safeColor()
                                        isMatched -> MaterialTheme.colorScheme.secondaryContainer
                                        index % 2 == 0 -> MaterialTheme.colorScheme.surface
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }

                                    ElevatedCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.elevatedCardColors(
                                            containerColor = backgroundColor
                                        ),
                                        onClick = {
                                            history.commit()

                                            selectedStorageIndex = if (isSelected) null else index
                                            matchIfPossible()
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                if (match != null && match.third > 0) {
                                                    Text(
                                                        text = "matched ${match.third}",
                                                        style = MaterialTheme.typography.labelSmall
                                                    )
                                                    Spacer(Modifier.height(4.dp))
                                                }

                                                Text(
                                                    field.key,
                                                    maxLines = 1,
                                                    style = MaterialTheme.typography.bodyLarge
                                                )
                                                Spacer(Modifier.height(2.dp))
                                                Text(
                                                    field.value,
                                                    maxLines = 1,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    // RIGHT: Requested
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Requested",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )

                        if (requestFields.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 180.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No requested fields", style = MaterialTheme.typography.bodyMedium)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 460.dp),
                                contentPadding = PaddingValues(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(
                                    items = requestFields,
                                    key = { index, field -> "${field.key}#$index" }
                                ) { index, field ->
                                    val isSelected = selectedRequestIndex == index
                                    val match = matches.firstOrNull { it.second == index }
                                    val isMatched = match != null

                                    val backgroundColor = when {
                                        isSelected -> MaterialTheme.colorScheme.secondaryContainer
                                        isMatched -> MaterialTheme.colorScheme.tertiaryContainer
                                        index % 2 == 0 -> MaterialTheme.colorScheme.surface
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }

                                    ElevatedCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.elevatedCardColors(
                                            containerColor = backgroundColor
                                        ),
                                        onClick = {
                                            history.commit()

                                            selectedRequestIndex = if (isSelected) null else index
                                            matchIfPossible()
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                if (match != null && match.third > 0) {
                                                    Text(
                                                        text = "matched ${match.third}\n",
                                                        style = MaterialTheme.typography.labelSmall
                                                    )
                                                    Spacer(Modifier.height(4.dp))
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        field.key,
                                                        maxLines = 1,
                                                        style = MaterialTheme.typography.bodyLarge
                                                    )
                                                }
                                                /*
                                                Spacer(Modifier.height(2.dp))
                                                Text(
                                                    field.keyAlias,
                                                    maxLines = 1,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.Unspecified
                                                )
                                                 */
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Add dialog
                if (openAddDialog) {
                    AddFieldDialog(
                        onDismiss = { openAddDialog = false },
                        onAddField = { newField ->
                            history.commit()

                            localStoredFields.add(newField)
                            onAddField(newField)
                            openAddDialog = false
                        }
                    )
                }
            }
        }
    }
}