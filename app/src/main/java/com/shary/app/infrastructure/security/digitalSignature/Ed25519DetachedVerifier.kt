package com.shary.app.infrastructure.security.digitalSignature

import com.shary.app.core.domain.interfaces.security.DetachedVerifier
import com.shary.app.utils.log.AppLogger
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer as BCSigner


class Ed25519DetachedVerifier : DetachedVerifier {
    override fun verify(message: ByteArray, signature: ByteArray, publicKey: ByteArray): Boolean {
        // Optional: harden with size checks (32B pub, 64B sig) before calling BC
        if (publicKey.size != 32 || signature.size != 64) return false
        val v = BCSigner()
        v.init(false, Ed25519PublicKeyParameters(publicKey, 0))
        v.update(message, 0, message.size)

        val ok = v.verifySignature(signature)
        AppLogger.debug("Ed25519DetachedVerifier", "event=verify_signature result=$ok")
        return ok
    }
}
