package com.shary.app.services.security.aes

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.*
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.SecretKeySpec

object AesCrypto {
    private const val AES_KEY_SIZE = 256
    private const val AES_ALGORITHM = "AES"
    private const val AES_MODE = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128

    // No almacenes nada en memoria si querés full determinismo.
    // Pero si preferís caching en sesión, podés reusarla.
    private var aesKey: SecretKey? = null

    // --------------------------------------------------------------------
    // Generación determinista de clave AES
    // --------------------------------------------------------------------
    fun deriveKey(password: String, username: String, timestamp: String): SecretKey {
        val input = "$password.$username"
        val salt = timestamp.toByteArray()
        val iterations = 100_000
        val keyLength = 256

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(input.toCharArray(), salt, iterations, keyLength)
        val keyBytes = factory.generateSecret(spec).encoded

        val key = SecretKeySpec(keyBytes, AES_ALGORITHM)
        aesKey = key // opcional: solo para sesión actual
        return key
    }

    fun encrypt(data: ByteArray, key: SecretKey = aesKey ?: error("AES key not initialized")): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance(AES_MODE)
        val iv = ByteArray(GCM_IV_LENGTH).apply { SecureRandom().nextBytes(this) }
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, spec)
        return iv to cipher.doFinal(data)
    }

    fun decrypt(iv: ByteArray, data: ByteArray, key: SecretKey = aesKey ?: error("AES key not initialized")): ByteArray {
        val cipher = Cipher.getInstance(AES_MODE)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        return cipher.doFinal(data)
    }

    // Opcional: para cache en sesión
    fun cacheAesKey(key: SecretKey) {
        aesKey = key
    }

    fun readAesKey(): SecretKey? {
        return aesKey
    }

    // Helpers opcionales si querés exportar
    fun keyToBase64(key: SecretKey): String =
        Base64.encodeToString(key.encoded, Base64.NO_WRAP)

    fun keyFromBase64(encoded: String): SecretKey =
        SecretKeySpec(Base64.decode(encoded, Base64.NO_WRAP), 0, 32, AES_ALGORITHM)
}
