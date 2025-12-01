package com.shary.app.core.domain.interfaces.services

interface CryptoService {
    fun encrypt(data: String): String
    fun decrypt(data: String): String
    fun generateKeyPair()
    fun gePublicKeyBase64(): String
}
