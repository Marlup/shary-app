package com.shary.app.core.domain.interfaces.security

interface AuthBackend {
    suspend fun registerIdentity(username: String, email: String, signPub: ByteArray, kexPub: ByteArray): Boolean
    suspend fun requestChallenge(username: String): ByteArray
    suspend fun verifyLogin(username: String, challenge: ByteArray, signature: ByteArray): Boolean
}
