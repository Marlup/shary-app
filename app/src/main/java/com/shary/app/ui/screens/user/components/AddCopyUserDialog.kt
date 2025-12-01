package com.shary.app.ui.screens.user.components

import androidx.compose.runtime.Composable
import com.shary.app.core.domain.models.UserDomain
import java.time.Instant

@Composable
fun AddCopyUserDialog(
    targetUser: UserDomain,
    onDismiss: () -> Unit,
    onAddUser: (UserDomain) -> Unit
) {
    val initial = UserDomain(
        username = "${targetUser.username} - Copy",
        email = "",
        dateAdded = Instant.EPOCH // will be set on confirm
    )

    UserEditorDialog(
        initial = initial,
        title = "Add New User",
        confirmLabel = "ADD",
        onDismiss = onDismiss,
        onConfirm = onAddUser
    )
}
