package com.shary.app.ui.screens.field.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shary.app.core.domain.types.enums.Tag
import com.shary.app.ui.components.SharyModalAction
import com.shary.app.ui.components.SharyModalComplexity
import com.shary.app.ui.components.SharyModalDestructiveAction
import com.shary.app.ui.components.SharyModalScaffold
import com.shary.app.ui.components.TagChip
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun FieldDetailDialog(
    keyName: String,
    value: String,
    valueItems: List<String> = emptyList(),
    alias: String,
    tag: Tag,
    typedValueLabel: String?,
    dateAdded: Instant,
    onCopyValue: () -> Unit,
    onDuplicateField: () -> Unit,
    onEditTag: () -> Unit,
    onEdit: () -> Unit,
    onDeleteField: () -> Unit,
    showMcpShareable: Boolean = false,
    isMcpShareable: Boolean = false,
    onMcpShareableChange: (Boolean) -> Unit = {},
    onDismiss: () -> Unit
) {
    SharyModalScaffold(
        title = keyName,
        subtitle = typedValueLabel,
        onDismissRequest = onDismiss,
        complexity = SharyModalComplexity.Compact,
        primaryAction = SharyModalAction(
            label = "Edit",
            onClick = onEdit
        ),
        destructiveAction = SharyModalDestructiveAction(
            label = "Delete",
            confirmationTitle = "Delete $keyName?",
            confirmationMessage =
                "This field will be removed from this vault. You can undo immediately afterward.",
            onConfirm = onDeleteField
        )
    ) {
        Text(
            text = "Value",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (valueItems.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                valueItems.forEach { item ->
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Alias",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = alias.ifBlank { "-" },
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Tag",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TagChip(tag = tag)

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Added",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = formatFieldDate(dateAdded),
            style = MaterialTheme.typography.bodyLarge
        )

        if (showMcpShareable) {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Allow MCP access",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = "Permit authorized agents to read or update this field.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isMcpShareable,
                    onCheckedChange = onMcpShareableChange
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(16.dp))

        Text(
            text = "Field actions",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(8.dp))

        FilledTonalButton(
            onClick = onCopyValue,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
        ) {
            Text("Copy value", maxLines = 2)
        }

        Spacer(Modifier.height(8.dp))

        FilledTonalButton(
            onClick = onDuplicateField,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
        ) {
            Text("Duplicate", maxLines = 2)
        }

        Spacer(Modifier.height(8.dp))

        FilledTonalButton(
            onClick = onEditTag,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
        ) {
            Text("Change tag", maxLines = 2)
        }
    }
}

private fun formatFieldDate(instant: Instant): String {
    if (instant == Instant.EPOCH) return "Unknown"
    return DateTimeFormatter
        .ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(Locale.getDefault())
        .withZone(ZoneId.systemDefault())
        .format(instant)
}
