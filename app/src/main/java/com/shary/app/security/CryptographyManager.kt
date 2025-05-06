package com.shary.app.security

import android.content.Context
import android.util.Base64
import android.util.Log
import com.shary.app.security.SecurityConstants.PATH_FILE_AUTHENTICATION
import com.shary.app.security.securityUtils.SecurityUtils.hashSecret
import com.shary.app.security.securityUtils.SecurityUtils.makeUserSalt
import java.io.File
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import javax.crypto.Cipher
import org.json.JSONObject
import java.math.BigInteger
import java.nio.charset.StandardCharsets

object CryptographyManager {

    private var privateKey: PrivateKey? = null
    private var publicKey: PublicKey? = null
    private var secretKey: String? = null

    private const val keySize = 2048

    // --- Public Key Handling ---
    fun hashPassword(password: String, userKey: String): ByteArray {
        val salt = makeUserSalt(userKey)
        return hashSecret(password.toByteArray(), salt)
    }

    fun getPubKeyToString(): String {
        return publicKey?.encoded?.let { Base64.encodeToString(it, Base64.NO_WRAP) } ?: ""
    }

    fun getPubKeyFromString(pubKeyStr: String): PublicKey? {
        val decoded = Base64.decode(pubKeyStr, Base64.NO_WRAP)
        val spec = X509EncodedKeySpec(decoded)
        val keyFactory = KeyFactory.getInstance("RSA")
        return keyFactory.generatePublic(spec)
    }

    // --- Key Management ---

    private fun generateKeys() {
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(keySize)
        val keyPair = keyPairGenerator.generateKeyPair()
        publicKey = keyPair.public
        privateKey = keyPair.private
    }

    private fun storeKeys(context: Context) {
        val privPath = File(context.filesDir, SecurityConstants.PATH_PRIVATE_KEY)
        val pubPath = File(context.filesDir, SecurityConstants.PATH_PUBLIC_KEY)
        val secretPath = File(context.filesDir, SecurityConstants.PATH_SECRET_KEY)

        privateKey?.let {
            privPath.writeBytes(it.encoded)
        }
        publicKey?.let {
            pubPath.writeBytes(it.encoded)
        }
        secretKey?.let {
            secretPath.writeText(it)
        }
    }

    fun loadKeys(context: Context) {
        val privPath = File(context.filesDir, SecurityConstants.PATH_PRIVATE_KEY)
        val pubPath = File(context.filesDir, SecurityConstants.PATH_PUBLIC_KEY)

        if (privPath.exists() && pubPath.exists()) {
            val privKeyBytes = privPath.readBytes()
            val pubKeyBytes = pubPath.readBytes()

            val keyFactory = KeyFactory.getInstance("RSA")

            privateKey = keyFactory.generatePrivate(PKCS8EncodedKeySpec(privKeyBytes))
            publicKey = keyFactory.generatePublic(X509EncodedKeySpec(pubKeyBytes))
        } else {
            generateKeys()
            storeKeys(context)
        }
    }

    fun loadSecretKey(context: Context): String {
        val secretPath = File(context.filesDir, SecurityConstants.PATH_SECRET_KEY)
        return if (secretPath.exists()) secretPath.readText() else ""
    }

    fun generateKeysFromSecrets(password: String, username: String, keySize: Int = 1024) {
        val seed = hashPassword(password, username)

        val rng = DeterministicRNG(seed)
        val e = BigInteger.valueOf(65537)
        val halfBits = keySize / 2

        val p = rng.getPrime(halfBits)
        var q = rng.getPrime(halfBits)
        while (p == q) {
            q = rng.getPrime(halfBits)
        }

        val n = p.multiply(q)
        val phi = (p.subtract(BigInteger.ONE)).multiply(q.subtract(BigInteger.ONE))
        val d = e.modInverse(phi)

        val publicSpec = java.security.spec.RSAPublicKeySpec(n, e)
        val privateSpec = java.security.spec.RSAPrivateKeySpec(n, d)

        val keyFactory = KeyFactory.getInstance("RSA")
        publicKey = keyFactory.generatePublic(publicSpec)
        privateKey = keyFactory.generatePrivate(privateSpec)

        Log.d("CryptographyManager", "RSA keys generated deterministically.")
        Log.d("CryptographyManager - publicKey", publicKey.toString())
        Log.d("CryptographyManager - privateKey", privateKey.toString())
    }

    // --- Crypto Core ---

    fun encrypt(plaintext: ByteArray, key: PublicKey? = publicKey): ByteArray {
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        return cipher.doFinal(plaintext)
    }

    fun decrypt(ciphertext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        return cipher.doFinal(ciphertext)
    }

    fun sign(message: ByteArray): ByteArray {
        val signature = Signature.getInstance("SHA256withRSA")
        signature.initSign(privateKey)
        signature.update(message)
        return signature.sign()
    }

    fun verify(message: ByteArray, signatureBytes: ByteArray, key: PublicKey? = publicKey): Boolean {
        val signature = Signature.getInstance("SHA256withRSA")
        signature.initVerify(key)
        signature.update(message)
        return signature.verify(signatureBytes)
    }

    fun saveSignature(context: Context, username: String, email: String, password: String) {
        if (username.isBlank() and username.isBlank() and email.isBlank() and password.isBlank()) {
            throw Error("At least one arguments is blank.")
        }
        generateKeysFromSecrets(password, username)

        val message = "$username.$email".toByteArray(StandardCharsets.UTF_8)
        val signature = sign(message)

        val json = JSONObject()
        json.put("message", Base64.encodeToString(message, Base64.NO_WRAP))
        json.put("signature", Base64.encodeToString(signature, Base64.NO_WRAP))

        val file = File(context.filesDir, PATH_FILE_AUTHENTICATION)
        file.writeText(json.toString())

        Log.d("CryptographyManager", "User signature stored.")
    }

    // --- Utilities ---

}
