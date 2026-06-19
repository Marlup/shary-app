package com.shary.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shary.app.ui.theme.SelectionCardBg
import com.shary.app.ui.theme.SharyRadius
import com.shary.app.ui.theme.SharyTextStyles
import com.shary.app.ui.theme.SurfaceWhite
import com.shary.app.ui.theme.TagVioletBg
import com.shary.app.ui.theme.Violet200
import com.shary.app.ui.theme.Violet50
import com.shary.app.ui.theme.Violet500
import com.shary.app.ui.theme.Violet600
import com.shary.app.ui.theme.Violet900

@Composable
fun RecipientBlob(name: String, email: String) {
    Row(
        modifier = Modifier
            .background(TagVioletBg, androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BoxAvatar(name = name)
        Spacer(Modifier.size(8.dp))
        Column {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                color = Violet900
            )
            Text(
                text = email,
                style = MaterialTheme.typography.bodySmall,
                color = Violet500
            )
        }
    }
}

@Composable
private fun BoxAvatar(name: String) {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    val initials = when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts.first().take(2).uppercase()
        else -> "${parts[0].first()}${parts[1].first()}".uppercase()
    }
    Row(
        modifier = Modifier
            .size(30.dp)
            .background(Violet600, SharyRadius.avatar),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White
        )
    }
}

@Composable
fun ReviewSectionCard(
    sectionLabel: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = sectionLabel.uppercase(),
            style = SharyTextStyles.sectionLabel,
            color = Violet500,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = SharyRadius.card,
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            border = BorderStroke(1.dp, Violet200),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                content = content
            )
        }
    }
}

@Composable
fun ReviewKeyValueRow(
    key: String,
    value: String,
    last: Boolean = false
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = key,
                style = MaterialTheme.typography.bodyMedium,
                color = Violet500
            )
            Text(
                text = value,
                style = SharyTextStyles.fieldValueMedium,
                color = Violet900
            )
        }
        if (!last) {
            HorizontalDivider(color = Violet50, thickness = 1.dp)
        }
    }
}

@Composable
fun SendMethodCard(
    icon: ImageVector,
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    onDisabledClick: (() -> Unit)? = null
) {
    val borderColor = animateColorAsState(
        targetValue = if (isSelected) Violet600 else Violet200,
        animationSpec = tween(180),
        label = "method-border"
    )
    val bgColor = animateColorAsState(
        targetValue = if (isSelected) SelectionCardBg else SurfaceWhite,
        animationSpec = tween(180),
        label = "method-bg"
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = true,
                onClick = {
                    if (enabled) onClick() else onDisabledClick?.invoke()
                }
            ),
        shape = SharyRadius.card,
        border = BorderStroke(1.5.dp, borderColor.value.copy(alpha = if (enabled) 1f else 0.5f)),
        colors = CardDefaults.cardColors(
            containerColor = bgColor.value.copy(alpha = if (enabled) 1f else 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) Violet600 else Violet600.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.size(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = if (enabled) Violet900 else Violet900.copy(alpha = 0.6f)
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) Violet500 else Violet500.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
