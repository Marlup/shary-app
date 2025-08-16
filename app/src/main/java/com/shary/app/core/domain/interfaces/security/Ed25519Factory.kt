package com.shary.app.core.domain.interfaces.security

import com.shary.app.infrastructure.security.sign.Ed25519Signer

interface Ed25519Factory {
    fun signerFromSeed(seed32: ByteArray): Ed25519Signer
    fun publicKeyFromSeed(seed32: ByteArray): ByteArray
}