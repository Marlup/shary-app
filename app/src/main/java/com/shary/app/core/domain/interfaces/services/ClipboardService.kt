package com.shary.app.core.domain.interfaces.services

interface ClipboardService {
    fun copyText(
        label: String,
        text: String,
        sensitive: Boolean,
        clearAfterSeconds: Int
    )

    fun clearOwnedClip()
}
