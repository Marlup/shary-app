package com.shary.app.infrastructure.security.local

import android.util.Base64
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

    private fun encGeneric(
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

    private fun decGeneric(
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

    fun encrypt(
        plain: ByteArray,
        u: String,
        p: CharArray,
        purpose: Purpose,
        aad: ByteArray?
    ): ByteArray = encGeneric(plain, u, p, purpose.code, aad)

    fun decrypt(
        blob: ByteArray,
        u: String,
        p: CharArray,
        purpose: Purpose,
        aad: ByteArray?
    ): ByteArray = decGeneric(blob, u, p, purpose.code, aad)

    // -------------------- Back‑compat String <-> Base64 wrappers --------------------
    // Note: these DO NOT use AAD to preserve legacy behavior.

    private fun encToB64(plain: String, u: String, p: CharArray, purpose: String, aad: ByteArray?): String {
        val key = deriveLocalKey(u, p, purpose)
        val (iv, body, tag) = cipher.encrypt(key, plain.encodeToByteArray())
        val blob = pack(iv, body, tag)
        return Base64.encodeToString(blob, Base64.NO_WRAP)
    }

    private fun decFromB64(b64: String, u: String, p: CharArray, purpose: String, aad: ByteArray?): String {
        val blob = Base64.decode(b64, Base64.DEFAULT)
        val (iv, body, tag) = unpack(blob)
        val key = deriveLocalKey(u, p, purpose)
        return cipher.decrypt(key, iv, body, tag).decodeToString()
    }

    // === Required String API (compat) ===

    fun encryptToString(
        plain: String,
        u: String,
        p: CharArray,
        purpose: String,
        aad: ByteArray?
    ): String = encToB64(plain, u, p, purpose, aad)

    fun decryptToString(
        b64: String,
        u: String,
        p: CharArray,
        purpose: String,
        aad: ByteArray?
    ): String = decFromB64(b64, u, p, purpose, aad)


    /** Credenciales JSON: formatea a bytes/JSON aprovechando la API genérica. */
    fun encryptCredentialsJson(u: String, safe: String, json: JSONObject, aad: ByteArray? = u.encodeToByteArray()): ByteArray =
        encGeneric(
            json.toString().encodeToByteArray(),
            u,
            safe.toCharArray(),
            Purpose.Credentials.code,
            aad
        )

    fun decryptCredentialsJson(u: String, safe: String, blob: ByteArray, aad: ByteArray? = u.encodeToByteArray()): JSONObject =
        JSONObject(
            decGeneric(
                blob,
                u,
                safe.toCharArray(),
                Purpose.Credentials.code,
                aad
            ).decodeToString())
}
