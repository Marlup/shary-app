package com.shary.app.infrastructure.security.local

import android.util.Base64
import android.util.Log
import com.shary.app.core.domain.types.valueobjects.Purpose
import com.shary.app.infrastructure.security.cipher.AesGcmCipher
import com.shary.app.infrastructure.security.derivation.KeyDerivation
import org.json.JSONObject

/**
 * LocalVault
 *
 * Derives local symmetric keys per-purpose:
 *   master = KDF(username, passwordChars, appId)  // 32B
 *   key    = HKDF(master, info="shary:local:<purpose>", len=32)
 *
 * Offers:
 *  - Binary API (bytes<->bytes) used by storage code (iv(12)||body||tag(16))
 *  - Back‑compat String<->Base64 helpers for key/value/alias/tag (no AAD)
 */
class LocalVault(
    private val kd: KeyDerivation,
    private val cipher: AesGcmCipher,
    private val appId: String = "com.shary.app"
) {
    private companion object {
        const val IV_LEN = 12
        const val TAG_LEN = 16
    }

    /** Derive a per‑purpose local key from username+password. */
    fun deriveLocalKey(
        username: String,
        password: CharArray,
        purpose: String = Purpose.Key.code
    ): ByteArray {
        Log.w("LocalVault", "deriveLocalKey - username: $username")
        Log.w("LocalVault", "deriveLocalKey - password: $password")
        Log.w("LocalVault", "deriveLocalKey - purpose: $purpose")
        val master = kd.masterSeed(username, password, appId)
        return kd.hkdf.expand(master, info = "shary:local:$purpose".toByteArray(), len = 32)
    }

    // -------------------- Binary helpers (bytes <-> bytes) --------------------

    private fun pack(iv: ByteArray, body: ByteArray, tag: ByteArray): ByteArray {
        require(iv.size == IV_LEN) { "Invalid IV length" }
        require(tag.size == TAG_LEN) { "Invalid TAG length" }
        return ByteArray(IV_LEN + body.size + TAG_LEN).apply {
            System.arraycopy(iv, 0, this, 0, IV_LEN)
            System.arraycopy(body, 0, this, IV_LEN, body.size)
            System.arraycopy(tag, 0, this, IV_LEN + body.size, TAG_LEN)
        }
    }

    private fun unpack(blob: ByteArray): Triple<ByteArray, ByteArray, ByteArray> {
        require(blob.size >= IV_LEN + TAG_LEN) { "Cipher blob too short" }
        val iv   = blob.copyOfRange(0, IV_LEN)
        val tag  = blob.copyOfRange(blob.size - TAG_LEN, blob.size)
        val body = blob.copyOfRange(IV_LEN, blob.size - TAG_LEN)
        return Triple(iv, body, tag)
    }

    private fun encryptGeneric(
        plain: ByteArray,
        localKey: ByteArray,
        aad: ByteArray?
    ): ByteArray {
        val (iv, body, tag) = cipher.encrypt(localKey, plain, aad)
        return pack(iv, body, tag)
    }

    private fun decryptGeneric(
        blob: ByteArray,
        localKey: ByteArray,
        aad: ByteArray?
    ): ByteArray {
        val (iv, body, tag) = unpack(blob)
        return cipher.decrypt(localKey, iv, body, tag, aad)
    }

    // Public binary API (kept as in your project)

    fun encrypt(
        plain: ByteArray,
        localKey: ByteArray,
        aad: ByteArray?
    ): ByteArray = encryptGeneric(plain, localKey, aad)

    fun decrypt(
        blob: ByteArray,
        localKey: ByteArray,
        aad: ByteArray?
    ): ByteArray = decryptGeneric(blob, localKey, aad)

    // -------------------- Back‑compat String <-> Base64 wrappers --------------------
    // Note: these DO NOT use AAD to preserve legacy behavior.

    private fun encryptToB64(plain: String, localKey: ByteArray, aad: ByteArray?): String {
        val (iv, body, tag) = cipher.encrypt(localKey, plain.encodeToByteArray())
        val blob = pack(iv, body, tag)
        return Base64.encodeToString(blob, Base64.NO_WRAP)
    }

    private fun decryptFromB64(b64: String, localKey: ByteArray, aad: ByteArray?): String {
        val blob = Base64.decode(b64, Base64.DEFAULT)
        val (iv, body, tag) = unpack(blob)
        return cipher.decrypt(localKey, iv, body, tag).decodeToString()
    }

    // === Required String API (compat) ===

    fun encryptToString(
        plain: String,
        localKey: ByteArray,
        aad: ByteArray?
    ): String = encryptToB64(plain, localKey, aad)

    fun decryptToString(
        b64: String,
        localKey: ByteArray,
        aad: ByteArray?
    ): String = decryptFromB64(b64, localKey, aad)


    /** Credenciales JSON: formatea a bytes/JSON aprovechando la API genérica. */
    fun encryptCredentials(u: String, localKey: ByteArray, json: JSONObject, aad: ByteArray? = u.encodeToByteArray()): ByteArray =
        encryptGeneric(
            json.toString().encodeToByteArray(),
            localKey,
            aad
        )

    fun decryptCredentials(u: String, localKey: ByteArray, blob: ByteArray, aad: ByteArray? = u.encodeToByteArray()): JSONObject =
        JSONObject(
            decryptGeneric(
                blob,
                localKey,
                aad
            ).decodeToString())

    private fun encryptGenericByDerivation(
        plain: ByteArray,
        u: String,
        p: CharArray,
        purpose: String,
        aad: ByteArray?
    ): ByteArray {
        val key = deriveLocalKey(u, p, purpose)
        val (iv, body, tag) = cipher.encrypt(key, plain, aad)
        return pack(iv, body, tag)
    }

    private fun decryptGenericByDerivation(
        blob: ByteArray,
        u: String,
        p: CharArray,
        purpose: String,
        aad: ByteArray?
    ): ByteArray {
        val (iv, body, tag) = unpack(blob)
        val key = deriveLocalKey(u, p, purpose)
        return cipher.decrypt(key, iv, body, tag, aad)
    }

    // Public binary API (kept as in your project)

    fun encryptByDerivation(
        plain: ByteArray,
        u: String,
        p: CharArray,
        purpose: Purpose,
        aad: ByteArray?
    ): ByteArray = encryptGenericByDerivation(plain, u, p, purpose.code, aad)

    fun decryptByDerivation(
        blob: ByteArray,
        u: String,
        p: CharArray,
        purpose: Purpose,
        aad: ByteArray?
    ): ByteArray = decryptGenericByDerivation(blob, u, p, purpose.code, aad)

    // -------------------- Back‑compat String <-> Base64 wrappers --------------------
    // Note: these DO NOT use AAD to preserve legacy behavior.

    private fun encryptToB64ByDerivation(plain: String, u: String, p: CharArray, purpose: String, aad: ByteArray?): String {
        val key = deriveLocalKey(u, p, purpose)
        val (iv, body, tag) = cipher.encrypt(key, plain.encodeToByteArray())
        val blob = pack(iv, body, tag)
        return Base64.encodeToString(blob, Base64.NO_WRAP)
    }

    private fun decryptFromB64ByDerivation(b64: String, u: String, p: CharArray, purpose: String, aad: ByteArray?): String {
        val blob = Base64.decode(b64, Base64.DEFAULT)
        val (iv, body, tag) = unpack(blob)
        val key = deriveLocalKey(u, p, purpose)
        return cipher.decrypt(key, iv, body, tag).decodeToString()
    }

    // === Required String API (compat) ===

    fun encryptToStringByDerivation(
        plain: String,
        u: String,
        p: CharArray,
        purpose: String,
        aad: ByteArray?
    ): String = encryptToB64ByDerivation(plain, u, p, purpose, aad)

    fun decryptToStringByDerivation(
        b64: String,
        u: String,
        p: CharArray,
        purpose: String,
        aad: ByteArray?
    ): String = decryptFromB64ByDerivation(b64, u, p, purpose, aad)


    /** Credenciales JSON: formatea a bytes/JSON aprovechando la API genérica. */
    fun encryptCredentialsByDerivation(u: String, safe: String, json: JSONObject, aad: ByteArray? = u.encodeToByteArray()): ByteArray =
        encryptGenericByDerivation(
            json.toString().encodeToByteArray(),
            u,
            safe.toCharArray(),
            Purpose.Credentials.code,
            aad
        )

    fun decryptCredentialsByDerivation(u: String, safe: String, blob: ByteArray, aad: ByteArray? = u.encodeToByteArray()): JSONObject =
        JSONObject(
            decryptGenericByDerivation(
                blob,
                u,
                safe.toCharArray(),
                Purpose.Credentials.code,
                aad
            ).decodeToString())
}
