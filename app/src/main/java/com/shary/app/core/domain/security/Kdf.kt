package com.shary.app.core.domain.security


interface Kdf {
    fun derive(password: CharArray, salt: ByteArray, lengthBytes: Int, iterations: Int = 200_000): ByteArray
}
