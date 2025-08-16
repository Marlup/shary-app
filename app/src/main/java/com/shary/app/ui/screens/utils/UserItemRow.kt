package com.shary.app.ui.screens.utils

import androidx.compose.runtime.Composable
import com.shary.app.core.domain.models.UserDomain


@Composable
fun UserItemRow(
    user: UserDomain,
    onEditClick: () -> Unit,
    onAddItemCopyClick: (() -> Unit)? = null
) {
    ItemRowBase(
        title = user.username,
        subtitle = user.email,
        tooltip = "", // or any extra details you want to show
        copyText = "${user.username} <${user.email}>",
        onEditClick = onEditClick,
        onAddItemCopyClick = onAddItemCopyClick
    )
}
