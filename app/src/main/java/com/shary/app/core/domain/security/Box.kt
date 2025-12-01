package com.shary.app.core.domain.security


interface Box {
    data class Sealed(
        val ephPublicKey: ByteArray,
        val iv: ByteArray,
        val ciphertext: ByteArray,
        val tag: ByteArray
    )
    fun seal(
        plain: ByteArray,
        myPrivate: ByteArray,
        peerPublic: ByteArray,
        aad: ByteArray? = null
    ): Sealed
    fun open(
        sealed: Sealed,
        myPrivate: ByteArray,
        peerPublic: ByteArray,
        aad: ByteArray? = null
    ): ByteArray
}
