package com.shary.app.ui.screens.user.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.shary.app.core.domain.models.UserDomain
import com.shary.app.ui.screens.utils.LongPressHint
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserEditorDialog(
    initial: UserDomain,
    title: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (UserDomain) -> Unit
) {
    var draft by remember(initial) { mutableStateOf(initial) }
    var showErrors by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val sheetMaxHeight = (configuration.screenHeightDp * 0.7f).dp
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // very light validation (you can plug your own)
    fun isValid(): Boolean =
        draft.username.isNotBlank() && draft.email.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = sheetMaxHeight)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(16.dp))

            Text(
                "Required",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = draft.username,
                onValueChange = { draft = draft.copy(username = it) },
                label = { Text("Username *") },
                isError = showErrors && draft.username.isBlank(),
                supportingText = {
                    if (showErrors && draft.username.isBlank()) {
                        Text("Required")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = draft.email,
                onValueChange = { draft = draft.copy(email = it) },
                label = { Text("Email *") },
                isError = showErrors && draft.email.isBlank(),
                supportingText = {
                    if (showErrors && draft.email.isBlank()) {
                        Text("Required")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                LongPressHint("Close without saving this user") {
                    OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                }
                LongPressHint("Save user changes") {
                    FilledTonalButton(
                        onClick = {
                            showErrors = true
                            if (!isValid()) return@FilledTonalButton
                            val normalized = draft.copy(
                                username = draft.username.trim(),
                                email = draft.email.trim(),
                                // dateAdded is set only if missing (EPOCH sentinel)
                                dateAdded = if (draft.dateAdded == Instant.EPOCH) Instant.now() else draft.dateAdded
                            )
                            onConfirm(normalized)
                            onDismiss()
                        }
                    ) { Text(confirmLabel) }
                }
            }
        }
    }
}
