package com.shary.app.ui.screens.field.utils.dialogs

import androidx.compose.runtime.Composable
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.types.enums.UiFieldTag
import java.time.Instant

/**
 * Thin wrapper over FieldEditorDialog to create a brand-new field.
 * Keeps UI consistent and reuse the same editor as "copy" flow.
 */
@Composable
fun AddFieldDialog(
    onDismiss: () -> Unit,
    onAddField: (FieldDomain) -> Unit,
    allTags: List<UiFieldTag>
) {
    // Seed empty domain model (dateAdded set on confirm inside FieldEditorDialog)
    val initial = FieldDomain(
        key = "",
        value = "",
        keyAlias = null,
        tag = UiFieldTag.Unknown,
        dateAdded = Instant.EPOCH
    )

    FieldEditorDialog(
        initial = initial,
        allTags = allTags,
        title = "Add new field",
        confirmLabel = "Add",
        onDismiss = onDismiss,
        onConfirm = onAddField
    )
}
