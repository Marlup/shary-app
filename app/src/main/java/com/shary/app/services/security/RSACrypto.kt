package com.shary.app.services.security

import android.content.Context
import android.util.Base64
import android.util.Log
import com.shary.app.services.security.securityUtils.SecurityUtils
import com.shary.app.services.security.securityUtils.SecurityUtils.hashSecret
import com.shary.app.services.security.securityUtils.SecurityUtils.makeUserSalt
import org.json.JSONObject
import java.io.File
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.*
import java.security.spec.*
import javax.crypto.Cipher

object RsaCrypto {
    private const val KEY_SIZE = 2048

    private var privateKey: PrivateKey? = null
    private var publicKey: PublicKey? = null

    fun generateKeys() {
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(KEY_SIZE)
        val keyPair = generator.generateKeyPair()
        publicKey = keyPair.public
        privateKey = keyPair.private
    }

    fun generateDeterministicKeyPair(password: String, username: String, keySize: Int = 2048): KeyPair {
        val seed = SecurityUtils.hashSecret(password.toByteArray(), SecurityUtils.makeUserSalt(username))
        val random = SecureRandom.getInstance("SHA1PRNG").apply { setSeed(seed) }

        val e = BigInteger.valueOf(65537L)
        val p = BigInteger.probablePrime(keySize / 2, random)
        var q = BigInteger.probablePrime(keySize / 2, random)
        while (q == p) q = BigInteger.probablePrime(keySize / 2, random)

        val n = p.multiply(q)
        val phi = p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE))
        val d = e.modInverse(phi)

        val dp = d.remainder(p.subtract(BigInteger.ONE))
        val dq = d.remainder(q.subtract(BigInteger.ONE))
        val qInv = q.modInverse(p)

        val publicSpec = RSAPublicKeySpec(n, e)
        val privateSpec = RSAPrivateCrtKeySpec(n, e, d, p, q, dp, dq, qInv)

        val keyFactory = KeyFactory.getInstance("RSA")
        val publicKey = keyFactory.generatePublic(publicSpec)
        val privateKey = keyFactory.generatePrivate(privateSpec)

        return KeyPair(publicKey, privateKey)
    }


    fun generateKeysFromSecrets(password: String, username: String, keySize: Int = 2048) {
        val keyPair = generateDeterministicKeyPair(password, username, keySize)
        publicKey = keyPair.public
        privateKey = keyPair.private

        Log.d("RsaCrypto", "Deterministic RSA keys generated.")
    }

    fun loadKeys(context: Context) {
        val privPath = File(context.filesDir, SecurityConstants.PATH_PRIVATE_KEY)
        val pubPath = File(context.filesDir, SecurityConstants.PATH_PUBLIC_KEY)
        if (privPath.exists() && pubPath.exists()) {
            val keyFactory = KeyFactory.getInstance("RSA")
            privateKey = keyFactory.generatePrivate(PKCS8EncodedKeySpec(privPath.readBytes()))
            publicKey = keyFactory.generatePublic(X509EncodedKeySpec(pubPath.readBytes()))
        } else {
            generateKeys()
            storeKeys(context)
        }
    }

    fun storeKeys(context: Context) {
        Log.d("storeKeys", privateKey.toString())
        Log.d("storeKeys", publicKey.toString())
        privateKey?.let {
            File(context.filesDir, SecurityConstants.PATH_PRIVATE_KEY).writeBytes(it.encoded)
        }
        publicKey?.let {
            File(context.filesDir, SecurityConstants.PATH_PUBLIC_KEY).writeBytes(it.encoded)
        }
    }

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

    fun publicKeyToString(): String = Base64.encodeToString(publicKey?.encoded, Base64.NO_WRAP)
    fun publicKeyFromString(encoded: String): PublicKey {
        val bytes = Base64.decode(encoded, Base64.NO_WRAP)
        return KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(bytes))
    }

    fun saveSignature(context: Context, username: String, email: String, password: String) {
        generateKeysFromSecrets(password, username)
        val message = "$username.$email".toByteArray(StandardCharsets.UTF_8)
        val signature = sign(message)

        val json = JSONObject().apply {
            put("message", Base64.encodeToString(message, Base64.NO_WRAP))
            put("signature", Base64.encodeToString(signature, Base64.NO_WRAP))
        }

        File(context.filesDir, SecurityConstants.PATH_FILE_AUTHENTICATION).writeText(json.toString())
    }
}
