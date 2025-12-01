package com.shary.app.infrastructure.security.messageCipher

import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom

class AesGcmCipher {
    private val rng = SecureRandom()
    fun encrypt(key: ByteArray, plain: ByteArray, aad: ByteArray? = null): Triple<ByteArray, ByteArray, ByteArray> {
        val iv = ByteArray(12).also { rng.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        if (aad != null) cipher.updateAAD(aad)
        val ct = cipher.doFinal(plain)
        val tag = ct.takeLast(16).toByteArray()
        val body = ct.dropLast(16).toByteArray()
        return Triple(iv, body, tag)
    }
    fun decrypt(key: ByteArray, iv: ByteArray, body: ByteArray, tag: ByteArray, aad: ByteArray? = null): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        if (aad != null) cipher.updateAAD(aad)
        return cipher.doFinal(body + tag)
    }
}
