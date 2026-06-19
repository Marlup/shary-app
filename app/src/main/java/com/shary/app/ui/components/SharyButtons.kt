package com.shary.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.shary.app.ui.theme.DestructiveBg
import com.shary.app.ui.theme.DestructiveText
import com.shary.app.ui.theme.SharyRadius
import com.shary.app.ui.theme.SurfaceMid
import com.shary.app.ui.theme.Violet200
import com.shary.app.ui.theme.Violet600
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
            contentColor = androidx.compose.ui.graphics.Color.White,
            disabledContainerColor = Violet600.copy(alpha = 0.45f),
            disabledContentColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)
        ),
        modifier = modifier.heightIn(min = 48.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        }
        if (icon != null) {
            androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SharySoftButton(
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
            containerColor = SurfaceMid,
            contentColor = Violet800,
            disabledContainerColor = SurfaceMid.copy(alpha = 0.5f),
            disabledContentColor = Violet800.copy(alpha = 0.6f)
        ),
        modifier = modifier.heightIn(min = 46.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        }
        if (icon != null) {
            androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
        }
        Text(text = text, style = MaterialTheme.typography.labelMedium)
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
        modifier = modifier.heightIn(min = 46.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        }
        if (icon != null) {
            androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
        }
        Text(text = text, style = MaterialTheme.typography.labelMedium)
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
        modifier = modifier.heightIn(min = 40.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = Violet600
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
    onDisabledClick: (() -> Unit)? = null
) {
    val clickable = enabled || onDisabledClick != null
    val resolvedBackground = when {
        !enabled -> SurfaceMid.copy(alpha = 0.5f)
        selected -> Violet200
        else -> SurfaceMid
    }
    val resolvedTint = when {
        !enabled -> Violet800.copy(alpha = 0.5f)
        selected -> Violet600
        else -> Violet800
    }
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
            .size(32.dp)
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
            modifier = Modifier.size(16.dp)
        )
    }
}
