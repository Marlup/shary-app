package com.shary.app.ui.screens.summary

import com.shary.app.ui.screens.home.utils.SendOption

internal enum class ReviewPayloadKind {
    FIELD_VALUES,
    REQUESTED_KEYS
}

internal data class ReviewPrivacyCopy(
    val methodDescription: String,
    val disclosure: String,
    val sensitiveWarning: String?,
    val primaryAction: String
)

internal fun reviewPrivacyCopy(
    method: SendOption,
    payloadKind: ReviewPayloadKind,
    recipientCount: Int,
    sensitiveFieldCount: Int,
    cloudExpiryDays: Int
): ReviewPrivacyCopy = when (method) {
    SendOption.Cloud -> ReviewPrivacyCopy(
        methodDescription = "Encrypted on this device · expires in $cloudExpiryDays days",
        disclosure = "Shary encrypts a separate payload for each selected contact before upload. " +
            "Relay ciphertext is retained until its $cloudExpiryDays-day expiry.",
        sensitiveWarning = null,
        primaryAction = "Send encrypted via cloud"
    )

    SendOption.Email -> {
        val payloadDisclosure = when (payloadKind) {
            ReviewPayloadKind.FIELD_VALUES ->
                "Your selected app receives a readable JSON attachment containing these field values."
            ReviewPayloadKind.REQUESTED_KEYS ->
                "The readable JSON attachment contains requested field names only, not their values."
        }
        val recipientDisclosure = if (recipientCount > 1) {
            " All $recipientCount addresses are placed in To and may be visible to every recipient."
        } else {
            " The selected address is placed in To."
        }
        ReviewPrivacyCopy(
            methodDescription = "Readable JSON attachment",
            disclosure = "$payloadDisclosure$recipientDisclosure " +
                "Shary cannot confirm sending or delivery. Temporary local attachments are scheduled " +
                "for deletion after 24 hours; expired files are removed when Shary next starts.",
            sensitiveWarning = sensitiveFieldCount
                .takeIf { it > 0 }
                ?.let { count ->
                    "Warning: this attachment contains $count sensitive " +
                        if (count == 1) "field in readable form." else "fields in readable form."
                },
            primaryAction = "Choose sharing app"
        )
    }
}
