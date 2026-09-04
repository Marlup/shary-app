package com.shary.app.ui.screens.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.types.valueobjects.FieldValueContract
import com.shary.app.core.domain.types.valueobjects.FieldValueTypes
import com.shary.app.core.domain.types.valueobjects.SENSITIVE_MASK


@Composable
fun FieldItemRow(
    field: FieldDomain,
    titleColor: Color = Color.Unspecified,
    onEditClick: () -> Unit,
    onAddItemCopyClick: (() -> Unit)? = null
) {
    val sensitive = FieldValueTypes.isSensitive(field.value)
    val displayValue = if (sensitive) {
        SENSITIVE_MASK
    } else {
        FieldValueContract.parse(field.value).plainData
    }
    ItemRowBase(
        title = field.key,
        subtitle = "- $displayValue",
        tooltip = if (sensitive) SENSITIVE_MASK else field.keyAlias.orEmpty(),
        copyText = "${field.key}: $displayValue",
        titleColor = titleColor,
        onEditClick = onEditClick,
        onAddItemCopyClick = onAddItemCopyClick
    )
}
