package com.shary.app.core.domain.security


interface Signer {
    fun getPublicKey(): ByteArray
    fun sign(message: ByteArray): ByteArray
    fun verify(message: ByteArray, signature: ByteArray, publicKey: ByteArray): Boolean
}
