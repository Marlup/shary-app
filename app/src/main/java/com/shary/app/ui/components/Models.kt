package com.shary.app.ui.components

import androidx.compose.ui.graphics.vector.ImageVector

data class DockAction(
    val label: String,
    val icon: ImageVector? = null,
    val enabled: Boolean = true,
    val onClick: () -> Unit = {}
)
