package com.shary.app.infrastructure.security.sign

import com.shary.app.core.domain.security.Signer
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer as BCSigner

class Ed25519Signer private constructor(
    private val priv: Ed25519PrivateKeyParameters,
    private val pub: Ed25519PublicKeyParameters
) : Signer {
    companion object {
        fun fromSeed(seed32: ByteArray): Ed25519Signer {
            val priv = Ed25519PrivateKeyParameters(seed32, 0)
            val pub = priv.generatePublicKey()
            return Ed25519Signer(priv, pub)
        }
    }
    override fun getPublicKey(): ByteArray = pub.encoded
    override fun sign(message: ByteArray): ByteArray {
        val s = BCSigner()
        s.init(true, priv)
        s.update(message, 0, message.size)
        return s.generateSignature()
    }
    override fun verify(message: ByteArray, signature: ByteArray, publicKey: ByteArray): Boolean {
        val v = BCSigner()
        v.init(false, Ed25519PublicKeyParameters(publicKey, 0))
        v.update(message, 0, message.size)
        return v.verifySignature(signature)
    }
}
