package com.shary.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.shary.app.ui.theme.DestructiveBg
import com.shary.app.ui.theme.DestructiveText
import com.shary.app.ui.theme.SharyRadius
import com.shary.app.ui.theme.SurfaceMid
import com.shary.app.ui.theme.Violet200
import com.shary.app.ui.theme.Violet600
import com.shary.app.ui.theme.Violet700
import com.shary.app.ui.theme.Violet800

@Composable
fun SharyPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = SharyRadius.input,
        colors = ButtonDefaults.buttonColors(
            containerColor = Violet600,
            contentColor = Color.White,
            disabledContainerColor = Violet600.copy(alpha = 0.45f),
            disabledContentColor = Color.White.copy(alpha = 0.7f)
        ),
        modifier = modifier.height(48.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        }
        if (icon != null) {
            androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

@Composable
fun SharySoftButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    contentColor: Color = Violet800,
    containerColor: Color = SurfaceMid,
    minHeight: Dp = 46.dp,
    iconSize: Dp = 16.dp,
    iconSpacing: Dp = 8.dp,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    contentDescription: String? = null,
    onDisabledClick: (() -> Unit)? = null,
) {
    val receivesClick = enabled || onDisabledClick != null
    val resolvedContainerColor = if (enabled) containerColor else containerColor.copy(alpha = 0.5f)
    val resolvedContentColor = if (enabled) contentColor else contentColor.copy(alpha = 0.6f)
    Button(
        onClick = {
            if (enabled) onClick() else onDisabledClick?.invoke()
        },
        enabled = receivesClick,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = resolvedContainerColor,
            contentColor = resolvedContentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.5f),
            disabledContentColor = contentColor.copy(alpha = 0.6f)
        ),
        contentPadding = contentPadding,
        modifier = modifier
            .height(minHeight)
            .semantics {
                contentDescription?.let { this.contentDescription = it }
            }
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(iconSize))
        }
        if (icon != null) {
            androidx.compose.foundation.layout.Spacer(Modifier.size(iconSpacing))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

@Composable
fun SharyDestructiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = DestructiveBg,
            contentColor = DestructiveText,
            disabledContainerColor = DestructiveBg.copy(alpha = 0.6f),
            disabledContentColor = DestructiveText.copy(alpha = 0.6f)
        ),
        modifier = modifier.height(48.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        }
        if (icon != null) {
            androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

@Composable
fun SharyTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = Violet700,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SharyIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    onDisabledClick: (() -> Unit)? = null,
    buttonSize: Dp = 48.dp,
    iconSize: Dp = 20.dp,
    containerColor: Color? = null,
    contentColor: Color? = null,
) {
    val clickable = enabled || onDisabledClick != null
    val defaultBackground = when {
        !enabled -> SurfaceMid.copy(alpha = 0.5f)
        selected -> Violet200
        else -> SurfaceMid
    }
    val defaultTint = when {
        !enabled -> Violet800.copy(alpha = 0.5f)
        selected -> Violet600
        else -> Violet800
    }
    val baseBackground = containerColor ?: defaultBackground
    val baseTint = contentColor ?: defaultTint
    val resolvedBackground = if (enabled) baseBackground else baseBackground.copy(alpha = 0.5f)
    val resolvedTint = if (enabled) baseTint else baseTint.copy(alpha = 0.6f)
    androidx.compose.material3.IconButton(
        onClick = {
            if (enabled) {
                onClick()
            } else {
                onDisabledClick?.invoke()
            }
        },
        enabled = clickable,
        modifier = modifier
            .size(buttonSize)
            .minimumInteractiveComponentSize()
            .background(
                color = resolvedBackground,
                shape = SharyRadius.iconButton
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = resolvedTint,
            modifier = Modifier.size(iconSize)
        )
    }
}
