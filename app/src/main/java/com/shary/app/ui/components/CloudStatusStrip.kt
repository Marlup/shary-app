package com.shary.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shary.app.ui.theme.SurfaceLight
import com.shary.app.ui.theme.SurfaceWhite
import com.shary.app.ui.theme.Violet500
import com.shary.app.ui.theme.Violet600
import com.shary.app.ui.theme.Violet800
import com.shary.app.ui.theme.Violet900
import com.shary.app.ui.utils.CloudUiStatus

@Composable
fun CloudStatusStrip(
    status: CloudUiStatus,
    modifier: Modifier = Modifier,
    syncActive: Boolean = false,
    pendingCount: Int = 0
) {
    val title = if (syncActive) "Syncing" else status.label
    val detail = when {
        syncActive -> "Cloud inbox is loading. Local data remains usable."
        pendingCount > 0 -> "${pendingCount} pending cloud items. Review before import."
        else -> status.detail
    }
    val borderColor = if (status.ready) Violet600.copy(alpha = 0.35f) else Violet500.copy(alpha = 0.35f)
    val iconTint = if (status.ready) Violet600 else Violet500

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceWhite, RoundedCornerShape(10.dp))
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Cloud,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(18.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Cloud ${title.lowercase()}",
                style = MaterialTheme.typography.labelMedium,
                color = Violet900,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = Violet500,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = status.safetyLabel,
            style = MaterialTheme.typography.labelSmall,
            color = Violet800,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .background(SurfaceLight, RoundedCornerShape(999.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
