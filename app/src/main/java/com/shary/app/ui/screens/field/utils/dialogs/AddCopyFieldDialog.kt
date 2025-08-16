package com.shary.app.ui.screens.field.utils.dialogs

import androidx.compose.runtime.Composable
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.types.enums.UiFieldTag
import java.time.Instant

@Composable
fun AddCopyFieldDialog(
    targetField: FieldDomain,
    allTags: List<UiFieldTag>,
    onDismiss: () -> Unit,
    onAddField: (FieldDomain) -> Unit
) {
    // Seed with target values; change key and reset date to set on confirm
    val initial = targetField.copy(
        key = "${targetField.key} - Copy",
        dateAdded = Instant.EPOCH
    )

    FieldEditorDialog(
        initial = initial,
        allTags = allTags,
        title = "Copy field",
        confirmLabel = "Add",
        onDismiss = onDismiss,
        onConfirm = onAddField
    )
}
