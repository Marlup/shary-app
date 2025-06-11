package com.shary.app.services.security

import android.content.Context
import android.util.Base64
import com.shary.app.services.security.aes.AesCrypto
import com.shary.app.services.security.securityUtils.SecurityUtils
import javax.crypto.SecretKey

object CryptographyManager {

    private lateinit var rsa: RsaCrypto
    private lateinit var aes: AesCrypto
    private var aesKey: SecretKey? = null

    fun inject(rsaCrypto: RsaCrypto, aesCrypto: AesCrypto) {
        rsa = rsaCrypto
        aes = aesCrypto
    }

    fun initializeWithUserSecrets(context: Context, username: String, password: String) {
        rsa.generateKeysFromSecrets(password, username)
        rsa.storeKeys(context)

        aesKey = aes.loadEncrypted(context, rsa::decrypt)
        if (aesKey == null) {
            aesKey = aes.generateKey().also {
                aes.storeEncrypted(context, rsa::encrypt, it)
            }
        }
    }

    fun hashPassword(password: String, username: String): ByteArray {
        val salt = SecurityUtils.makeUserSalt(username)
        return SecurityUtils.hashSecret(password.toByteArray(), salt)
    }

    fun encryptKeyRSA(key: String): String =
        Base64.encodeToString(rsa.encrypt(key.toByteArray()), Base64.NO_WRAP)

    fun decryptKeyRSA(encoded: String): String =
        String(rsa.decrypt(Base64.decode(encoded, Base64.NO_WRAP)))

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

    fun getPubKeyToString(): String = rsa.publicKeyToString()
    fun getPubKeyFromString(pubKeyStr: String) = rsa.publicKeyFromString(pubKeyStr)

    fun saveSignature(context: Context, username: String, email: String, password: String) {
        rsa.saveSignature(context, username, email, password)
    }
}
