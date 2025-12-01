package com.shary.app.infrastructure.security.digitalSignature

import com.shary.app.core.domain.interfaces.security.Ed25519Factory

class BcEd25519Factory : Ed25519Factory {
    override fun signerFromSeed(seed32: ByteArray) = Ed25519Signer.fromSeed(seed32)
    override fun publicKeyFromSeed(seed32: ByteArray) =
        Ed25519Signer
            .fromSeed(seed32)
            .getPublicKey()
}