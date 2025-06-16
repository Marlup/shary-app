package com.shary.app.services.security

import android.content.Context
import android.util.Base64
import android.util.Log
import com.shary.app.core.constants.Constants.PATH_AUTH_SIGNATURE
import com.shary.app.services.security.securityUtils.SecurityUtils
import org.json.JSONObject
import java.io.File
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.*
import java.security.spec.*
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object RsaCrypto {
    private const val KEY_SIZE = 2048
    private const val KDF_ITERATIONS = 100_000
    private const val KDF_KEY_LENGTH = 256

    var privateKey: PrivateKey? = null
        private set
    var publicKey: PublicKey? = null
        private set

    // --------------------------------------------------------------------
    // Deterministic Key Generation from user + password + timestamp
    // --------------------------------------------------------------------
    fun generateKeysFromCredentials(username: String, password: String, timestamp: String) {
        val seed = deriveSeed(password, username, timestamp)
        Log.d("generateKeysFromCredentials - seed", timestamp)
        Log.d("RSA - generateKeysFromCredentials", seed.toString())
        val rng = DeterministicRNG(seed)
        generateKeysFromSecrets(rng)
    }

    private fun deriveSeed(password: String, username: String, timestamp: String): ByteArray {
        val input = "$password.$username"
        val salt = timestamp.toByteArray()
        val spec = PBEKeySpec(input.toCharArray(), salt, KDF_ITERATIONS, KDF_KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    fun generateKeysFromSecrets(rng: DeterministicRNG, keySize: Int = KEY_SIZE) {
        val keyPair = generateDeterministicRSAKeyPair(rng, keySize)
        publicKey = keyPair.public
        privateKey = keyPair.private

        Log.d("generateKeysFromSecrets - publicKey", publicKey.toString())
        Log.d("generateKeysFromSecrets - privKey", privateKey.toString())

        Log.d("RsaCrypto", "Deterministic RSA keys generated (no storage).")
    }

    private fun generateDeterministicRSAKeyPair(rng: DeterministicRNG, keySize: Int): KeyPair {
        val e = BigInteger.valueOf(65537L)
        val p = rng.nextPrime(keySize / 2)
        var q = rng.nextPrime(keySize / 2)
        while (q == p) q = rng.nextPrime(keySize / 2)

        val n = p.multiply(q)
        val phi = p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE))
        val d = e.modInverse(phi)

        val dp = d.remainder(p.subtract(BigInteger.ONE))
        val dq = d.remainder(q.subtract(BigInteger.ONE))
        val qInv = q.modInverse(p)

        val publicSpec = RSAPublicKeySpec(n, e)
        val privateSpec = RSAPrivateCrtKeySpec(n, e, d, p, q, dp, dq, qInv)
        val keyFactory = KeyFactory.getInstance("RSA")
        return KeyPair(
            keyFactory.generatePublic(publicSpec),
            keyFactory.generatePrivate(privateSpec)
        )
    }

    // --------------------------------------------------------------------
    // Encryption / Decryption
    // --------------------------------------------------------------------
    fun encrypt(data: ByteArray, key: PublicKey? = publicKey): ByteArray {
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        return cipher.doFinal(data)
    }

    fun decrypt(data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        return cipher.doFinal(data)
    }

    // --------------------------------------------------------------------
    // Signing / Verification
    // --------------------------------------------------------------------
    fun sign(data: ByteArray): ByteArray {
        val signature = Signature.getInstance("SHA256withRSA")
        signature.initSign(privateKey)
        signature.update(data)
        return signature.sign()
    }

    fun verify(data: ByteArray, signatureBytes: ByteArray, key: PublicKey? = publicKey): Boolean {
        val signature = Signature.getInstance("SHA256withRSA")
        signature.initVerify(key)
        signature.update(data)
        return signature.verify(signatureBytes)
    }

    fun saveSignature(context: Context, username: String, email: String, password: String) {
        val timestamp = SecurityUtils.loadOrCreateTimestamp(context)

        // Regenerar claves desde inputs (sin almacenamiento)
        generateKeysFromCredentials(username, password, timestamp)

        val message = "$username.$email".toByteArray(StandardCharsets.UTF_8)
        val signature = sign(message)

        val json = JSONObject().apply {
            put("message", Base64.encodeToString(message, Base64.NO_WRAP))
            put("signature", Base64.encodeToString(signature, Base64.NO_WRAP))
        }

        //val dir = File(context.filesDir, PATH_AUTH_SIGNATURE)
        //if (!dir.exists()) dir.mkdirs()

        File(context.filesDir, "data/authentication/signature.json").apply {
            parentFile?.mkdirs()
            writeText(json.toString())
        }

        Log.d("RsaCrypto", "Signature saved for user: $username")
    }

    // --------------------------------------------------------------------
    // Public Key export/import
    // --------------------------------------------------------------------
    fun publicKeyToString(): String =
        Base64.encodeToString(publicKey?.encoded, Base64.NO_WRAP)

    fun publicKeyFromString(encoded: String): PublicKey {
        val decoded = Base64.decode(encoded, Base64.NO_WRAP)
        return KeyFactory.getInstance("RSA")
            .generatePublic(X509EncodedKeySpec(decoded))
    }

    // --------------------------------------------------------------------
    // Optional: Ephemeral Signature Example (e.g. for login)
    // --------------------------------------------------------------------
    fun generateEphemeralSignature(username: String, email: String): JSONObject {
        val message = "$username.$email".toByteArray(StandardCharsets.UTF_8)
        val signature = sign(message)

        return JSONObject().apply {
            put("message", Base64.encodeToString(message, Base64.NO_WRAP))
            put("signature", Base64.encodeToString(signature, Base64.NO_WRAP))
        }
    }
}
