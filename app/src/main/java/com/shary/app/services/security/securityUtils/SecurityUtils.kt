package com.shary.app.services.security.securityUtils

import android.content.Context
import android.util.Base64
import android.util.Log
import com.shary.app.core.constants.Constants.PATH_AUTHENTICATION
import com.shary.app.core.constants.Constants.PATH_AUTH_SIGNATURE
import org.json.JSONObject
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object SecurityUtils {

    fun signatureFile(context: Context): File {
        val dir = File(context.filesDir, PATH_AUTH_SIGNATURE)
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "signature.json")
    }

    fun credentialsFile(context: Context): File {
        val dir = File(context.filesDir, PATH_AUTHENTICATION)
        if (!dir.exists()) dir.mkdirs()
        return File(dir, ".credentials")
    }

    private fun timestampFile(context: Context): File {
        val dir = File(context.filesDir, PATH_AUTHENTICATION)
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "credentials.timestamp")
    }

    private fun getCurrentUtc(format: String = "iso"): Any {
        val now = Date()
        return when (format) {
            "timestamp" -> now.time / 1000
            "datetime" -> now
            else -> SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(now)
        }
    }

    private fun getSha256Hash(message: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(message)
        return digest.digest()
    }

    fun hashMessage(message: String): ByteArray {
        val bytes = message.toByteArray(StandardCharsets.UTF_8)
        return getSha256Hash(bytes)
    }
    fun hashMessageToString(message: String): String {
        return hashMessage(message).joinToString("") { "%02x".format(it) }
    }

    fun hashMessageExtended(message: String): Pair<ByteArray, String> {
        val bytes = message.toByteArray(StandardCharsets.UTF_8)
        val hash = getSha256Hash(bytes)
        val hex = hash.joinToString("") { "%02x".format(it) }
        return Pair(hash, hex)
    }

    fun makeVerificationHash(data: String, secretKey: String, timestamp: Long? = null, nonce: String? = null): Triple<String, Long, String> {
        val currentTimestamp = getCurrentUtc("timestamp") as Long
        val usedTimestamp = timestamp ?: currentTimestamp

        if (timestamp != null && timestamp <= currentTimestamp) {
            throw IllegalArgumentException("Timestamp is too old")
        }

        val usedNonce = nonce ?: generateNonce()
        val message = "$data|$usedTimestamp|$usedNonce|$secretKey"

        val hash = hashMessageToString(message)
        return Triple(hash, usedTimestamp, usedNonce)
    }

    fun generateNonce(length: Int = 16): String {
        val random = SecureRandom()
        val nonce = ByteArray(length)
        random.nextBytes(nonce)
        return nonce.joinToString("") { "%02x".format(it) }
    }

    fun pbkdf2Hash(password: ByteArray, salt: ByteArray, iterations: Int = 100_000): ByteArray {
        val spec = PBEKeySpec(String(password, StandardCharsets.UTF_8).toCharArray(), salt, iterations, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    fun aesEncrypt(key: ByteArray, plaintext: String): ByteArray {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val iv = ByteArray(16)
        SecureRandom().nextBytes(iv)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
        val ciphertext = cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))
        return iv + ciphertext
    }

    fun aesDecrypt(key: ByteArray, encryptedData: ByteArray): String {
        val iv = encryptedData.copyOfRange(0, 16)
        val ciphertext = encryptedData.copyOfRange(16, encryptedData.size)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
        val plaintext = cipher.doFinal(ciphertext)
        return String(plaintext, StandardCharsets.UTF_8)
    }

    fun makeUserSalt(user: String): ByteArray {
        return user.toByteArray(StandardCharsets.UTF_8)
    }

    // --- Password hashing ---
    fun hashSecret(secret: ByteArray, salt: ByteArray, iterations: Int = 100_000): ByteArray {
        //val spec = PBEKeySpec(String(secret, StandardCharsets.UTF_8).toCharArray(), salt, iterations, 256)
        Log.d("SecurityUtils - secret", secret.toString())
        Log.d("SecurityUtils - salt", salt.toString())
        val spec = PBEKeySpec(String(secret, StandardCharsets.UTF_8).toCharArray(), salt, iterations, 128)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    // --- Base 64 ---
    fun base64Decode(encoded: String): ByteArray = Base64.decode(encoded, Base64.NO_WRAP)
    fun base64Encode(data: ByteArray): String = Base64.encodeToString(data, Base64.NO_WRAP)

    // --- Json Hash ---
    fun computeJsonHash(json: JSONObject): String {
        val sorted = JSONObject(json.toString()) // assuming sorted JSON
        val raw = sorted.toString().toByteArray()
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(raw).joinToString("") { String.format("%02x", it) }
    }

    fun getCurrentUtcTimestamp(): Long = System.currentTimeMillis() / 1000

    fun getTimestampAfterExpiry(baseTimestamp: Long = getCurrentUtcTimestamp(), extraTime: Int = 3600): Long {
        return baseTimestamp + extraTime
    }

    fun loadOrCreateTimestamp(context: Context): String {
        val file = timestampFile(context)
        return if (file.exists()) {
            file.readText()
        } else {
            val timestamp = System.currentTimeMillis().toString()
            file.writeText(timestamp)
            timestamp
        }
    }
}