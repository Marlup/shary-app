package com.shary.app.core.domain.types.valueobjects

data class Sealed(
    val ephPublicKey: ByteArray,
    val iv: ByteArray,
    val ciphertext: ByteArray,
    val tag: ByteArray
)