package com.shary.app.ui.screens.field.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditAttributes
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shary.app.core.domain.types.enums.Tag
import com.shary.app.ui.components.TagChip
import com.shary.app.ui.theme.SharyRadius
import com.shary.app.ui.theme.Violet200
import com.shary.app.ui.theme.Violet500
import com.shary.app.ui.theme.Violet600
import com.shary.app.ui.theme.Violet900

@Composable
fun FieldDetailDialog(
    keyName: String,
    value: String,
    valueItems: List<String> = emptyList(),
    alias: String,
    tag: Tag,
    typedValueLabel: String?,
    onCopyValue: () -> Unit,
    onDuplicateField: () -> Unit,
    onEditValue: () -> Unit,
    onEditTag: () -> Unit,
    onEditAlias: () -> Unit,
    onDeleteField: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = keyName,
                    style = MaterialTheme.typography.titleLarge,
                    color = Violet900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TagChip(tag = tag)
                    DetailTextAction(Icons.Default.LocalOffer, "Edit tag", onEditTag)
                    if (!typedValueLabel.isNullOrBlank()) {
                        Text(
                            text = typedValueLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = Violet500,
                            modifier = Modifier
                                .background(Violet200, SharyRadius.chip)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Value", style = MaterialTheme.typography.labelSmall, color = Violet500)
                        if (valueItems.isNotEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                valueItems.forEach { item ->
                                    Text(
                                        text = item,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Violet900,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Violet200.copy(alpha = 0.35f), SharyRadius.chip)
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = value,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Violet900
                            )
                        }
                    }
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        DetailTextAction(Icons.Default.ContentCopy, "Copy value", onCopyValue)
                        DetailTextAction(Icons.Default.EditNote, "Edit value", onEditValue)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Alias", style = MaterialTheme.typography.labelSmall, color = Violet500)
                        Text(
                            text = alias.ifBlank { "-" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Violet900
                        )
                    }
                    DetailTextAction(Icons.Default.EditAttributes, "Edit alias", onEditAlias)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        DetailTextAction(Icons.Default.FileCopy, "Duplicate", onDuplicateField)
                        DetailTextAction(Icons.Default.Delete, "Delete", onDeleteField)
                    }
                }
            }
        }
    )
}

@Composable
private fun DetailTextAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    TextButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Violet600
        )
        Spacer(Modifier.width(6.dp))
        Text(label, color = Violet600, style = MaterialTheme.typography.labelMedium)
    }
}
