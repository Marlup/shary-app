package com.shary.app.application.security

import com.shary.app.core.domain.interfaces.security.CryptographyManager


class DeriveIdentityKeysUseCase(private val cryptographyManager: CryptographyManager) {
    operator fun invoke(username: String, password: CharArray, appId: String) =
        cryptographyManager.deriveIdentity(username, password, appId)
}
