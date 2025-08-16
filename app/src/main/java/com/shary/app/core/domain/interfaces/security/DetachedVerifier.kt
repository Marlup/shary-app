package com.shary.app.core.domain.interfaces.security

interface DetachedVerifier {
    fun verify(message: ByteArray, signature: ByteArray, publicKey: ByteArray): Boolean
}