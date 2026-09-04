package com.shary.app.ui.screens.field

/** Keeps the onboarding nudge out of normal browsing and filtered results. */
internal fun shouldShowBuildVaultNudge(
    totalFieldCount: Int,
    query: String,
    activeFilter: String
): Boolean = totalFieldCount in 1..2 && query.isBlank() && activeFilter == "All"
