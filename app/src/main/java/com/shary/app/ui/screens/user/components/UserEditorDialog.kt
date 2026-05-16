package com.shary.app.ui.screens.user.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.shary.app.core.domain.models.UserDomain
import com.shary.app.ui.components.SharyPrimaryButton
import com.shary.app.ui.components.SharySoftButton
import com.shary.app.ui.theme.SharyRadius
import com.shary.app.ui.theme.SurfaceLight
import com.shary.app.ui.theme.Violet200
import com.shary.app.ui.theme.Violet500
import com.shary.app.ui.theme.Violet900
import com.shary.app.utils.Validation
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

    fun emailValidationMessage(): String =
        Validation.validateEmailSyntax(draft.email.trim())

    fun isValid(): Boolean =
        draft.username.isNotBlank() && emailValidationMessage().isEmpty()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = SharyRadius.sheet,
        containerColor = SurfaceLight,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 8.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .background(Violet200, RoundedCornerShape(99.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = sheetMaxHeight)
                .padding(horizontal = 18.dp, vertical = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                title,
                style = MaterialTheme.typography.headlineLarge,
                color = Violet900
            )
            Spacer(Modifier.height(16.dp))

            Text(
                "Required",
                style = MaterialTheme.typography.labelSmall,
                color = Violet500
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = draft.username,
                onValueChange = { draft = draft.copy(username = it) },
                label = { Text("Alias (local) *") },
                isError = showErrors && draft.username.isBlank(),
                supportingText = {
                    if (showErrors && draft.username.isBlank()) {
                        Text("Required")
                    } else {
                        Text("Only visible on your device")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            val emailError = if (showErrors) emailValidationMessage() else ""
            OutlinedTextField(
                value = draft.email,
                onValueChange = { draft = draft.copy(email = it) },
                label = { Text("Email *") },
                isError = emailError.isNotEmpty(),
                supportingText = {
                    if (emailError.isNotEmpty()) {
                        Text(emailError)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                SharySoftButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                SharyPrimaryButton(
                    text = confirmLabel,
                    onClick = {
                        showErrors = true
                        if (!isValid()) return@SharyPrimaryButton
                        val normalized = draft.copy(
                            username = draft.username.trim(),
                            email = draft.email.trim(),
                            dateAdded = if (draft.dateAdded == Instant.EPOCH) Instant.now() else draft.dateAdded
                        )
                        onConfirm(normalized)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
