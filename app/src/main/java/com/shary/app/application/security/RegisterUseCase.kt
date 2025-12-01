package com.shary.app.application.security

import com.shary.app.core.domain.interfaces.security.AuthBackend
import com.shary.app.core.domain.interfaces.security.CryptographyManager


class RegisterUseCase(
    private val cryptographyManager: CryptographyManager,
    private val authBackend: AuthBackend,
    private val appId: String = "com.shary.app"
) {
    suspend operator fun invoke(username: String, email: String, password: CharArray): Result<Unit> =
        runCatching {
            val id = cryptographyManager.deriveIdentity(username, password, appId)

            // We do not persist in this flux; upload public to the backend:
            val ok = authBackend.registerIdentity(username, email, id.getSignPublic(), id.getKexPublic())
            require(ok) { "Registration failed" }
        }
}
