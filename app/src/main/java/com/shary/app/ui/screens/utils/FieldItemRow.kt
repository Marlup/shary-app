package com.shary.app.ui.screens.utils

import androidx.compose.runtime.Composable
import com.shary.app.core.domain.models.FieldDomain


@Composable
fun FieldItemRow(
    field: FieldDomain,
    onEditClick: () -> Unit,
    onAddItemCopyClick: (() -> Unit)? = null
) {
    ItemRowBase(
        title = field.key,
        subtitle = "- ${field.value}",
        tooltip = field.keyAlias.orEmpty(),
        copyText = "${field.key}: ${field.value}",
        onEditClick = onEditClick,
        onAddItemCopyClick = onAddItemCopyClick
    )
}