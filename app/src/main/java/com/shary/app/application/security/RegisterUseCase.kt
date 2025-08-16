package com.shary.app.application.security

import com.shary.app.core.domain.interfaces.security.AuthService
import com.shary.app.core.domain.interfaces.security.CryptographyManager


class RegisterUseCase(
    private val cryptographyManager: CryptographyManager,
    private val auth: AuthService,
    private val appId: String = "com.shary.app"
) {
    suspend operator fun invoke(username: String, email: String, password: CharArray): Result<Unit> =
        runCatching {
            val id = cryptographyManager.deriveIdentity(username, password, appId)

            // We do not persist in this flux; upload public to the backend:
            val ok = auth.registerIdentity(username, email, id.getSignPublic(), id.getKexPublic())
            require(ok) { "Registration failed" }
        }
}
