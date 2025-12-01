package com.shary.app.application.security

import com.shary.app.core.domain.interfaces.security.CryptographyManager


class SealMessageUseCase(private val crypto: CryptographyManager) {
    operator fun invoke(payload: ByteArray, receiverKexPublic: ByteArray, username: String, password: CharArray, appId: String, nonce: ByteArray) =
        crypto.sealTo(payload, receiverKexPublic, username, password, appId, nonce)
}
