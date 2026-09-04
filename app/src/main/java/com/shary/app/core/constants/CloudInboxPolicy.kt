package com.shary.app.core.constants

object CloudInboxPolicy {
    const val DEFAULT_EXPIRY_DAYS = 7
    const val MAX_EXPIRY_DAYS = 14

    // Local-only review previews. Payloads are decrypted on device before the user accepts/rejects.
    const val ENABLE_REJECT_NOTIFY_SENDER = false
    const val ENABLE_FIELD_NAME_PREVIEW = true
}
