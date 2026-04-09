package com.shary.app.ui.screens.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.types.valueobjects.FieldValueContract


@Composable
fun FieldItemRow(
    field: FieldDomain,
    titleColor: Color = Color.Unspecified,
    onEditClick: () -> Unit,
    onAddItemCopyClick: (() -> Unit)? = null
) {
    val displayValue = FieldValueContract.parse(field.value).plainData
    ItemRowBase(
        title = field.key,
        subtitle = "- $displayValue",
        tooltip = field.keyAlias.orEmpty(),
        copyText = "${field.key}: $displayValue",
        titleColor = titleColor,
        onEditClick = onEditClick,
        onAddItemCopyClick = onAddItemCopyClick
    )
}
