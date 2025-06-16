package com.shary.app.services.security

import android.content.Context
import android.util.Base64
import android.util.Log
import com.shary.app.services.security.aes.AesCrypto
import com.shary.app.services.security.securityUtils.SecurityUtils
import com.shary.app.services.security.securityUtils.SecurityUtils.credentialsFile
import com.shary.app.services.security.securityUtils.SecurityUtils.loadOrCreateTimestamp
import com.shary.app.services.security.securityUtils.SecurityUtils.signatureFile
import javax.crypto.SecretKey

object CryptoManager {

    private lateinit var rsa: RsaCrypto
    private lateinit var aes: AesCrypto
    private var aesKey: SecretKey? = null

    // Inyección de dependencias para testing o modularidad
    fun inject(rsaCrypto: RsaCrypto, aesCrypto: AesCrypto) {
        rsa = rsaCrypto
        aes = aesCrypto
    }

    // --------------------------------------------------
    // Inicialización sin password
    // --------------------------------------------------
    fun initializeKeysWithUser(context: Context, username: String, password: String) {
        val timestamp = loadOrCreateTimestamp(context)
        Log.d("initializeKeysWithUser - timestamp", timestamp)

        rsa.generateKeysFromCredentials(username, password, timestamp)
        aesKey = aes.deriveKey(password, username, timestamp)
        aes.cacheAesKey(aesKey!!)
        Log.d("CryptoManager", "Initialized with user: $username")
    }

    // --------------------------------------------------
    // Inicialización con password
    // --------------------------------------------------
    fun initializeWithUserSecrets(context: Context, username: String, password: String) {
        val timestamp = loadOrCreateTimestamp(context)
        rsa.generateKeysFromCredentials(username, password, timestamp)
        aesKey = aes.deriveKey(password, username, timestamp)
        aes.cacheAesKey(aesKey!!)
        Log.d("CryptoManager", "Initialized with secrets for user: $username")
    }

    // --------------------------------------------------
    // AES Key helpers
    // --------------------------------------------------
    fun encryptValueAES(value: String): String {
        val (iv, encrypted) = aes.encrypt(value.toByteArray(), aesKey!!)
        return Base64.encodeToString(iv + encrypted, Base64.NO_WRAP)
    }

    fun decryptValueAES(encoded: String): String {
        val combined = Base64.decode(encoded, Base64.NO_WRAP)
        val iv = combined.sliceArray(0 until 12)
        val data = combined.sliceArray(12 until combined.size)
        return String(aes.decrypt(iv, data, aesKey!!))
    }

    // --------------------------------------------------
    // RSA Key helpers
    // --------------------------------------------------
    fun encryptKeyRSA(key: String): String =
        Base64.encodeToString(rsa.encrypt(key.toByteArray()), Base64.NO_WRAP)

    fun decryptKeyRSA(encoded: String): String =
        String(rsa.decrypt(Base64.decode(encoded, Base64.NO_WRAP)))

    // --------------------------------------------------
    // Public Key I/O
    // --------------------------------------------------
    fun getPubKeyToString(): String = rsa.publicKeyToString()
    fun getPubKeyFromString(pubKeyStr: String) = rsa.publicKeyFromString(pubKeyStr)

    // --------------------------------------------------
    // Hash y firma
    // --------------------------------------------------
    fun hashPassword(password: String, username: String): ByteArray {
        val salt = SecurityUtils.makeUserSalt(username)
        return SecurityUtils.hashSecret(password.toByteArray(), salt)
    }

    fun saveSignature(context: Context, username: String, email: String, password: String) {
        rsa.saveSignature(context, username, email, password)
    }
}
