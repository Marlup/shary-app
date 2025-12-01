package com.shary.app.infrastructure.security.auth

import com.shary.app.core.domain.interfaces.security.AuthBackend
import com.shary.app.core.domain.interfaces.security.CryptographyManager
import jakarta.inject.Singleton
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@Singleton
class InMemoryAuthBackend @Inject constructor(
    private val crypto: CryptographyManager
) : AuthBackend {
    private val users = ConcurrentHashMap<String, Pair<ByteArray, ByteArray>>()
    private val challenges = ConcurrentHashMap<String, ByteArray>()
    private val rng = SecureRandom()

    override suspend fun registerIdentity(username: String, email: String, signPub: ByteArray, kexPub: ByteArray): Boolean {
        users[username] = signPub to kexPub
        return true
    }

    override suspend fun requestChallenge(username: String): ByteArray {
        val challenge = ByteArray(32).also { rng.nextBytes(it) }
        challenges[username] = challenge
        return challenge
    }

    override suspend fun verifyLogin(username: String, challenge: ByteArray, signature: ByteArray): Boolean {
        val (signPub, _) = users[username] ?: return false
        val last = challenges[username] ?: return false
        if (!last.contentEquals(challenge)) return false
        return crypto.verifyDetached(challenge, signature, signPub)
    }
}
