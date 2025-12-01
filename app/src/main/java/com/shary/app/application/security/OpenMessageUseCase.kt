package com.shary.app.application.security

import com.shary.app.core.domain.security.Box
import com.shary.app.core.domain.interfaces.security.CryptographyManager


class OpenMessageUseCase(private val crypto: CryptographyManager) {
    operator fun invoke(sealed: Box.Sealed, senderPublic: ByteArray, username: String, password: CharArray, appId: String) =
        crypto.openFrom(sealed, senderPublic, username, password, appId)
}
