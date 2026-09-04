package com.shary.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shary.app.ui.theme.DestructiveBg
import com.shary.app.ui.theme.DestructiveText
import com.shary.app.ui.theme.SurfaceLight
import com.shary.app.ui.theme.SurfaceWhite
import com.shary.app.ui.theme.TagMintBg
import com.shary.app.ui.theme.TagMintText
import com.shary.app.ui.theme.TextMuted
import com.shary.app.ui.theme.Violet900
import com.shary.app.ui.theme.Violet700
import com.shary.app.ui.utils.CloudUiStatus

@Composable
fun CloudStatusStrip(
    status: CloudUiStatus,
    modifier: Modifier = Modifier,
    syncActive: Boolean = false,
    pendingCount: Int = 0
) {
    val visualTitle = when {
        syncActive -> "Cloud syncing"
        status.ready -> "Cloud ready"
        else -> status.label
    }
    val visualDetail = when {
        syncActive -> "Inbox loading"
        pendingCount > 0 -> "$pendingCount pending"
        status.ready -> "Tap inbox when needed"
        status.safetyLabel == "cloud-blocked" -> "Verify email"
        else -> "Local only"
    }
    val accessibilityDetail = when {
        syncActive -> "Cloud inbox is loading. Local data remains usable."
        pendingCount > 0 -> "$pendingCount pending cloud items. Review before import."
        else -> status.detail
    }
    val iconTint = when {
        syncActive -> Violet700
        status.ready -> TagMintText
        else -> DestructiveText
    }
    val iconBackground = when {
        syncActive -> SurfaceLight
        status.ready -> TagMintBg
        else -> DestructiveBg
    }
    val borderColor = iconTint.copy(alpha = 0.2f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceWhite, RoundedCornerShape(10.dp))
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(10.dp))
            .semantics {
                contentDescription = "Cloud status"
                stateDescription = "$visualTitle. $accessibilityDetail"
            }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .background(iconBackground, RoundedCornerShape(999.dp))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Cloud,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = visualTitle,
                style = MaterialTheme.typography.labelLarge,
                color = Violet900,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = visualDetail,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .background(SurfaceLight, RoundedCornerShape(999.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}
