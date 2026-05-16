package com.shary.app.ui.screens.utils

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.types.valueobjects.FieldValueContract
import com.shary.app.core.domain.types.valueobjects.MatchingState
import com.shary.app.ui.components.SharyIconButton
import com.shary.app.ui.components.SharyPrimaryButton
import com.shary.app.ui.components.SharySoftButton
import com.shary.app.ui.screens.field.components.AddFieldDialog
import com.shary.app.ui.theme.SelectionCardBg
import com.shary.app.ui.theme.SharyRadius
import com.shary.app.ui.theme.SurfaceLight
import com.shary.app.ui.theme.SurfaceWhite
import com.shary.app.ui.theme.Violet200
import com.shary.app.ui.theme.Violet500
import com.shary.app.ui.theme.Violet600
import com.shary.app.ui.theme.Violet900
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

    fun snapshotState(): MatchingState = MatchingState(
        selectedStorageIndex = selectedStorageIndex,
        selectedRequestIndex = selectedRequestIndex,
        isStorageFirst = isStorageFirst,
        matches = matches.toList(),
        freeLabelsFromUnmatched = freeLabelsFromUnmatched.toList(),
    )

    fun restoreState(state: MatchingState) {
        selectedStorageIndex = state.selectedStorageIndex
        selectedRequestIndex = state.selectedRequestIndex
        isStorageFirst = state.isStorageFirst

        matches.clear()
        matches.addAll(state.matches)

        freeLabelsFromUnmatched.clear()
        freeLabelsFromUnmatched.addAll(state.freeLabelsFromUnmatched)
    }

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

    val totalToMatch = requestFields.size
    val matchedCount = matches.size
    val isFullyMatched = totalToMatch > 0 && matchedCount == totalToMatch
    val progress = if (totalToMatch == 0) 0f else matchedCount.toFloat() / totalToMatch.toFloat()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 320.dp, max = 720.dp),
            shape = SharyRadius.dialog,
            color = SurfaceLight,
            border = BorderStroke(1.dp, Violet200),
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Match fields",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Violet900
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SharyIconButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Undo",
                            onClick = { if (history.canUndo) history.undo() },
                            modifier = Modifier
                        )
                        SharyIconButton(
                            icon = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Redo",
                            onClick = { if (history.canRedo) history.redo() },
                            modifier = Modifier
                        )
                        SharyIconButton(
                            icon = Icons.Default.Add,
                            contentDescription = "Add field",
                            onClick = { openAddDialog = true }
                        )
                    }
                }

                Text(
                    text = "$matchedCount/$totalToMatch matched",
                    style = MaterialTheme.typography.labelMedium,
                    color = Violet500,
                    modifier = Modifier.padding(top = 8.dp)
                )

                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .height(8.dp),
                    color = Violet600,
                    trackColor = Violet200
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        MatchColumn(
                            title = "Stored",
                            fields = localStoredFields,
                            selectedIndex = selectedStorageIndex,
                            matches = matches,
                            isStoredColumn = true,
                            onSelect = { index, isSelected ->
                                history.commit()
                                selectedStorageIndex = if (isSelected) null else index
                                matchIfPossible()
                            }
                        )
                    }

                    VerticalDivider(
                        modifier = Modifier
                            .heightIn(min = 120.dp)
                            .padding(horizontal = 8.dp),
                        color = Violet200
                    )

                    Box(modifier = Modifier.weight(1f)) {
                        MatchColumn(
                            title = "Requested",
                            fields = requestFields,
                            selectedIndex = selectedRequestIndex,
                            matches = matches,
                            isStoredColumn = false,
                            onSelect = { index, isSelected ->
                                history.commit()
                                selectedRequestIndex = if (isSelected) null else index
                                matchIfPossible()
                            }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SharySoftButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )
                    SharyPrimaryButton(
                        text = "Accept",
                        onClick = {
                            onAccept(buildAcceptedSelection())
                            onDismiss()
                        },
                        enabled = isFullyMatched,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

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

@Composable
private fun MatchColumn(
    title: String,
    fields: List<FieldDomain>,
    selectedIndex: Int?,
    matches: List<Triple<Int, Int, Int>>,
    isStoredColumn: Boolean,
    onSelect: (index: Int, isSelected: Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = Violet500,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )

        if (fields.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isStoredColumn) "No stored fields" else "No requested fields",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Violet500
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(fields, key = { index, field -> "${field.key}#$index" }) { index, field ->
                    val match = if (isStoredColumn) {
                        matches.firstOrNull { it.first == index }
                    } else {
                        matches.firstOrNull { it.second == index }
                    }
                    val isMatched = match != null
                    val isSelected = selectedIndex == index
                    val borderColor = if (isMatched || isSelected) Violet600 else Violet200
                    val bgColor = if (isMatched || isSelected) SelectionCardBg else SurfaceWhite

                    Card(
                        onClick = { onSelect(index, isSelected) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = bgColor),
                        border = BorderStroke(1.5.dp, borderColor),
                        shape = SharyRadius.card,
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = field.key,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Violet900,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (isStoredColumn) {
                                    Text(
                                        text = FieldValueContract.parse(field.value).plainData,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Violet500,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            match?.let {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .background(Violet600, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = it.third.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
