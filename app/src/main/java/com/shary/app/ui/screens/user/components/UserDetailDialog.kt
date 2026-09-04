package com.shary.app.ui.screens.user.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shary.app.core.domain.models.UserDomain
import com.shary.app.core.domain.types.enums.Tag
import com.shary.app.ui.components.SharyModalAction
import com.shary.app.ui.components.SharyModalComplexity
import com.shary.app.ui.components.SharyModalDestructiveAction
import com.shary.app.ui.components.SharyModalScaffold
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun UserDetailDialog(
    user: UserDomain,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onCopyEmail: () -> Unit,
    onCreateSimilar: () -> Unit,
    onDelete: () -> Unit,
    onChangeTag: () -> Unit = {},
    flowAction: SharyModalAction? = null
) {
    val displayName = user.username.ifBlank { user.email }
    val addedLabel = remember(user.dateAdded) { formatContactDate(user.dateAdded) }

    SharyModalScaffold(
        title = displayName,
        subtitle = "Stored locally in this vault",
        onDismissRequest = onDismiss,
        complexity = SharyModalComplexity.Compact,
        primaryAction = SharyModalAction(
            label = "Edit",
            onClick = onEdit
        ),
        destructiveAction = SharyModalDestructiveAction(
            label = "Delete",
            confirmationTitle = "Delete $displayName?",
            confirmationMessage =
                "This contact will be removed from this vault. You can undo immediately afterward.",
            onConfirm = onDelete
        )
    ) {
        Text(
            text = "Email",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = user.email,
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Added",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = addedLabel,
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Group",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = if (user.tag == Tag.Unknown) "Unknown" else user.tag.toTagString(),
            style = MaterialTheme.typography.bodyLarge
        )

        flowAction?.let { action ->
            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(16.dp))
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Share with this contact",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Button(
                        onClick = action.onClick,
                        enabled = action.enabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Text(action.label, maxLines = 2)
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(16.dp))

        Text(
            text = "Contact actions",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(8.dp))

        FilledTonalButton(
            onClick = onCopyEmail,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
        ) {
            Text("Copy email", maxLines = 2)
        }

        Spacer(Modifier.height(8.dp))

        FilledTonalButton(
            onClick = onCreateSimilar,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
        ) {
            Text("Create similar", maxLines = 2)
        }

        Spacer(Modifier.height(8.dp))

        FilledTonalButton(
            onClick = onChangeTag,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
        ) {
            Text("Change tag", maxLines = 2)
        }
    }
}

private fun formatContactDate(instant: Instant): String {
    if (instant == Instant.EPOCH) return "Unknown"
    return DateTimeFormatter
        .ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(Locale.getDefault())
        .withZone(ZoneId.systemDefault())
        .format(instant)
}
