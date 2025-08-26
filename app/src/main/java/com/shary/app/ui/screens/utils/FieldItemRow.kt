package com.shary.app.ui.screens.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.shary.app.core.domain.models.FieldDomain


@Composable
fun FieldItemRow(
    field: FieldDomain,
    titleColor: Color = Color.Unspecified,
    onEditClick: () -> Unit,
    onAddItemCopyClick: (() -> Unit)? = null
) {
    ItemRowBase(
        title = field.key,
        subtitle = "- ${field.value}",
        tooltip = field.keyAlias.orEmpty(),
        copyText = "${field.key}: ${field.value}",
        titleColor = titleColor,
        onEditClick = onEditClick,
        onAddItemCopyClick = onAddItemCopyClick
    )
}