package com.shary.app.ui.screens

/**
 * Describes the next visible action in the cross-screen field send flow.
 *
 * The current screen must have a local selection before a CTA is exposed.
 * Once both a payload and recipients are selected, the flow continues through
 * the existing review screen rather than sending immediately.
 */
internal data class SendFlowCta(
    val label: String,
    val contentDescription: String,
    val destination: SendFlowDestination
)

internal enum class SendFlowDestination {
    CONTACTS,
    FIELDS,
    FIELD_REVIEW,
    REQUEST_REVIEW
}

internal fun fieldSendFlowCta(
    hasSelectedFields: Boolean,
    hasSelectedUsers: Boolean
): SendFlowCta? = when {
    !hasSelectedFields -> null
    !hasSelectedUsers -> SendFlowCta(
        label = "Select Contacts",
        contentDescription = "Select contacts to receive the selected fields",
        destination = SendFlowDestination.CONTACTS
    )
    else -> SendFlowCta(
        label = "Review & send",
        contentDescription = "Review selected fields and contacts",
        destination = SendFlowDestination.FIELD_REVIEW
    )
}

internal fun userSendFlowCta(
    hasSelectedUsers: Boolean,
    hasSelectedFields: Boolean,
    hasSelectedRequestFields: Boolean
): SendFlowCta? = when {
    !hasSelectedUsers -> null
    hasSelectedRequestFields -> SendFlowCta(
        label = "Review & send",
        contentDescription = "Review selected request and contacts",
        destination = SendFlowDestination.REQUEST_REVIEW
    )
    hasSelectedFields -> SendFlowCta(
        label = "Review & send",
        contentDescription = "Review selected fields and contacts",
        destination = SendFlowDestination.FIELD_REVIEW
    )
    else -> SendFlowCta(
        label = "Select Fields",
        contentDescription = "Select fields to send to the selected contacts",
        destination = SendFlowDestination.FIELDS
    )
}
