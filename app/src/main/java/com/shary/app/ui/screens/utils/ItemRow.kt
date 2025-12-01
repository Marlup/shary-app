package com.shary.app.ui.screens.utils

import android.content.ClipData
import android.graphics.drawable.shapes.Shape
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.shary.app.ui.screens.utils.Constants.FIELD_TOOLTIP_ALIVE_DURATION
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
@Composable
fun ItemRowBase(
    title: String,
    subtitle: String,
    tooltip: String = "",
    copyText: String = "$title: $subtitle",
    titleColor: Color = Color.Unspecified,
    onEditClick: () -> Unit,
    onAddItemCopyClick: (() -> Unit)? = null // null hides "Add Copy"
) {
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var menuExpanded by remember { mutableStateOf(false) }
    var showTooltip by remember { mutableStateOf(false) }

    LaunchedEffect(showTooltip) {
        if (showTooltip) {
            delay(Constants.FIELD_TOOLTIP_ALIVE_DURATION)
            showTooltip = false
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(0.6f)
                .padding(end = 8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = titleColor,
                modifier = Modifier.padding(bottom = 2.dp) // spacing below, optional
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp) // <<< padding INSIDE
                )
            }

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Right menu
        Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Filled.MoreHoriz,
                    contentDescription = "Open Actions Menu",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = { menuExpanded = false; onEditClick() },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                )

                // Show Add Copy only when provided
                if (onAddItemCopyClick != null) {
                    DropdownMenuItem(
                        text = { Text("Add Copy") },
                        onClick = { menuExpanded = false; onAddItemCopyClick() },
                        leadingIcon = { Icon(Icons.Filled.CopyAll, contentDescription = "Add Copy") }
                    )
                }

                DropdownMenuItem(
                    text = { Text("Copy") },
                    onClick = {
                        menuExpanded = false
                        scope.launch {
                            clipboard.setClipEntry(
                                ClipEntry(ClipData.newPlainText("row", copyText))
                            )
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    },
                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = "Copy") }
                )

                if (tooltip.isNotBlank()) {
                    DropdownMenuItem(
                        text = { Text("Show Details") },
                        onClick = { menuExpanded = false; showTooltip = true },
                        leadingIcon = { Icon(Icons.Default.Info, contentDescription = "Info") }
                    )
                }
            }
        }

        if (showTooltip) {
            Popup(
                alignment = Alignment.TopEnd,
                offset = IntOffset(0, -80),
                onDismissRequest = { showTooltip = false }
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .padding(4.dp)
                        .wrapContentSize()
                ) {
                    Text(
                        text = tooltip,
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}
