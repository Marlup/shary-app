package com.shary.app.ui.utils

import com.shary.app.core.domain.interfaces.states.CloudState

data class CloudUiStatus(
    val ready: Boolean,
    val reason: String,
    val label: String,
    val detail: String,
    val safetyLabel: String
)

fun cloudUiStatus(state: CloudState): CloudUiStatus {
    val ready = state.isOnline && state.isUserValidated
    return when {
        !state.isOnline -> CloudUiStatus(
            ready = false,
            reason = "Cloud offline",
            label = "Offline",
            detail = "Cloud actions stay local until connection returns.",
            safetyLabel = "local-only"
        )
        !state.isUserValidated -> CloudUiStatus(
            ready = false,
            reason = "Email not verified for cloud",
            label = "Verify email",
            detail = "Verify your email before syncing or reviewing cloud inbox.",
            safetyLabel = "cloud-blocked"
        )
        else -> CloudUiStatus(
            ready = true,
            reason = "",
            label = "Ready",
            detail = "Cloud inbox and secure relay are available.",
            safetyLabel = "cloud-ready"
        )
    }
}
