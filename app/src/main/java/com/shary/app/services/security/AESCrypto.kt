package com.shary.app.services.security.aes

import android.content.Context
import android.util.Base64
import com.shary.app.services.security.SecurityConstants
import java.io.File
import java.security.SecureRandom
import javax.crypto.*
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object AesCrypto {
    private const val AES_KEY_SIZE = 256
    private const val AES_ALGORITHM = "AES"
    private const val AES_MODE = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128

    private var aesKey: SecretKey? = null

    fun generateKey(): SecretKey {
        val keyGen = KeyGenerator.getInstance(AES_ALGORITHM)
        keyGen.init(AES_KEY_SIZE)
        return keyGen.generateKey()
    }

    fun loadOrCreateKey(): SecretKey {
        if (aesKey == null) aesKey = generateKey()
        return aesKey!!
    }

    fun encrypt(data: ByteArray, key: SecretKey = loadOrCreateKey()): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance(AES_MODE)
        val iv = ByteArray(GCM_IV_LENGTH).apply { SecureRandom().nextBytes(this) }
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, spec)
        return iv to cipher.doFinal(data)
    }

    fun decrypt(iv: ByteArray, data: ByteArray, key: SecretKey = loadOrCreateKey()): ByteArray {
        val cipher = Cipher.getInstance(AES_MODE)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        return cipher.doFinal(data)
    }

    fun storeEncrypted(context: Context, rsaEncryptor: (ByteArray) -> ByteArray, key: SecretKey) {
        val encrypted = rsaEncryptor(keyToBase64(key).toByteArray())
        File(context.filesDir, SecurityConstants.PATH_SECRET_KEY).writeBytes(encrypted)
    }

    fun loadEncrypted(context: Context, rsaDecryptor: (ByteArray) -> ByteArray): SecretKey? {
        val file = File(context.filesDir, SecurityConstants.PATH_SECRET_KEY)
        if (!file.exists()) return null
        val decrypted = rsaDecryptor(file.readBytes())
        return keyFromBase64(String(decrypted))
    }

    fun keyToBase64(key: SecretKey): String =
        Base64.encodeToString(key.encoded, Base64.NO_WRAP)

    fun keyFromBase64(encoded: String): SecretKey =
        SecretKeySpec(Base64.decode(encoded, Base64.NO_WRAP), 0, 32, AES_ALGORITHM)
}
