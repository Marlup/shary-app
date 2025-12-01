package com.shary.app.infrastructure.security.shared.keyExchange

import org.bouncycastle.crypto.params.X25519PrivateKeyParameters
import org.bouncycastle.crypto.params.X25519PublicKeyParameters

data class X25519KeyPair(
    val priv: X25519PrivateKeyParameters,
    val pub: X25519PublicKeyParameters
) {
    fun privateEncoded() = priv.encoded
    fun publicEncoded() = pub.encoded


    companion object {
        fun fromSeed(seed32: ByteArray): X25519KeyPair {
            val priv = X25519PrivateKeyParameters(seed32, 0)
            val pub = priv.generatePublicKey()
            return X25519KeyPair(priv, pub)
        }
    }


    fun ecdh(peerPublic: ByteArray): ByteArray {
        val secret = ByteArray(32)
        priv.generateSecret(X25519PublicKeyParameters(peerPublic, 0), secret, 0)
        return secret
    }
}
