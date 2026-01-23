package com.shary.app.infrastructure.security.helper

import com.shary.app.infrastructure.security.derivation.KeyDerivation
import com.shary.app.infrastructure.security.digitalSignature.Ed25519Signer

class IdentityKeys(
    private val kd: KeyDerivation
) {
    fun sign(message: ByteArray, email: String, password: CharArray, appId: String): ByteArray {
        val master = kd.masterSeed(email, password, appId)
        val seed = kd.idSignSeed(master)
        val signer = Ed25519Signer.fromSeed(seed)
        return signer.sign(message)
    }
    fun public(email: String, password: CharArray, appId: String): ByteArray {
        val master = kd.masterSeed(email, password, appId)
        val seed = kd.idSignSeed(master)
        return Ed25519Signer.fromSeed(seed).getPublicKey()
    }
}
