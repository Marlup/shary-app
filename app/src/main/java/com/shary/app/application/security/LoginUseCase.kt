// :application/security/LoginUseCase.kt
package com.shary.app.application.security

import com.shary.app.core.domain.interfaces.security.AuthBackend
import com.shary.app.core.domain.interfaces.security.CryptographyManager
import com.shary.app.infrastructure.security.sign.Ed25519Signer

class LoginUseCase(
    private val cryptographyManager: CryptographyManager,
    private val authBackend: AuthBackend,
    private val appId: String = "com.shary.app"
) {
    suspend operator fun invoke(username: String, password: CharArray): Result<Unit> =
        runCatching {

            // 1) Derive identity to obtain sign seed (we do not store anything)
            val id = cryptographyManager.deriveIdentity(username, password, appId)

            // 2) Request challenge to the backend
            val challenge = authBackend.requestChallenge(username)

            // 3) Locally sign  through Ed25519 by using signSeed
            val signer = Ed25519Signer.fromSeed(id.getSignSeed())
            val signature = signer.sign(challenge)

            // 4) Send verification
            val ok = authBackend.verifyLogin(username, challenge, signature)
            require(ok) { "Invalid credentials" }
        }
}
