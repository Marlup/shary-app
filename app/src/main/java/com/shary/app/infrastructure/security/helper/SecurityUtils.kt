package com.shary.app.infrastructure.security.helper

import android.content.Context
import android.content.pm.PackageManager
import android.util.Base64
import android.util.AtomicFile
import com.shary.app.core.constants.Constants.PATH_AUTHENTICATION
import com.shary.app.core.constants.Constants.PATH_AUTH_SIGNATURE
import com.shary.app.infrastructure.services.cloud.Constants.TIME_ALIVE_FIREBASE_DOCUMENT

import org.json.JSONObject
import com.shary.app.utils.log.AppLogger
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlin.math.log

/**
 * SecurityUtils (v2)
 *
 * Responsibility:
 * - Filesystem helpers (paths).
 * - Small utilities (base64, timestamp, JSON hash).
 * - NO cryptography primitives here (those live in CryptographyManager).
 */
object SecurityUtils {

    /** Identificador estable de la app para namespacing en la KDF. */
    fun appId(context: Context): String {
        return try {
            context.packageManager.getApplicationInfo(
                context.packageName,
                PackageManager.GET_META_DATA
            )
            context.packageName
        } catch (_: Throwable) {
            context.packageName
        }
    }

    // ---------------- Hashing ----------------


    fun hashMessage(message: String): ByteArray {
        // Aplica hash seguro (SHA-256 o superior)
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(message.toByteArray(Charsets.UTF_8))
    }

    fun hashPassword(email: String, password: CharArray, appId: String): ByteArray {
        // Concatenate and normalize the data
        val input = buildString {
            append(email.trim().lowercase())
            append(':')
            append(password.concatToString())
            append(':')
            append(appId.trim().lowercase())
        }

        // Aplica hash seguro (SHA-256 o superior)
        return hashMessage(input)
    }

    /**
     * Variante que devuelve en Base64 para transporte o almacenamiento textual.
     */
    fun hashPasswordB64(email: String, password: CharArray, appId: String): String =
        base64Encode(hashPassword(email, password, appId))

    fun hashMessageB64(message: String): String {
        val hashB64 = base64Encode(hashMessage(message))
        AppLogger.debug("SecurityUtils", "event=hash_message_b64")
        return hashB64
    }

    fun hashSaltedMessage(message: String, salt: String): ByteArray {
        return runCatching {
            throw UnsupportedOperationException("fallback")
        }.getOrElse {
            val md = MessageDigest.getInstance("SHA-256")
            md.update(message.toByteArray(StandardCharsets.UTF_8))
            md.update(':'.code.toByte())
            md.update(salt.toByteArray(StandardCharsets.UTF_8))
            md.digest()
        }
    }

    // ---------------- Paths ----------------
    fun signatureFile(context: Context): File {
        val dir = File(context.filesDir, PATH_AUTH_SIGNATURE)
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "signature.json")
    }

    fun credentialsFile(context: Context): File {
        val dir = File(context.filesDir, PATH_AUTHENTICATION)
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "credentials.v3")
    }

    fun legacyCredentialsFile(context: Context): File {
        val dir = File(context.filesDir, PATH_AUTHENTICATION)
        if (!dir.exists()) dir.mkdirs()
        return File(dir, ".credentials")
    }

    fun credentialsLockFile(context: Context): File {
        val dir = File(context.filesDir, PATH_AUTHENTICATION)
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "credentials.locked")
    }

    private fun timestampFile(context: Context): File {
        val dir = File(context.filesDir, PATH_AUTHENTICATION)
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "credentials.timestamp")
    }

    // ---------------- Time ----------------
    /** Seconds since epoch (UTC). */
    fun getCurrentUtcTimestamp(): Long = System.currentTimeMillis() / 1000

    /** Returns a timestamp after [extraTime] seconds (default 1h). */
    fun getTimestampAfterExpiry(baseTimestamp: Long = getCurrentUtcTimestamp(), extraTime: Long = TIME_ALIVE_FIREBASE_DOCUMENT): Long {
        AppLogger.debug("SecurityUtils", "event=timestamp_after_expiry")
        return baseTimestamp + extraTime
    }

    /** Loads or creates a simple timestamp marker used by the app. */
    fun loadOrCreateTimestamp(context: Context): String {
        val file = timestampFile(context)
        return if (file.exists()) file.readText() else {
            val ts = System.currentTimeMillis().toString()
            writeTextAtomic(file, ts)
            ts
        }
    }

    // ---------------- Atomic writes ----------------
    fun writeBytesAtomic(file: File, bytes: ByteArray) {
        val atomic = AtomicFile(file)
        var output = atomic.startWrite()
        try {
            output.write(bytes)
            atomic.finishWrite(output)
        } catch (e: Throwable) {
            atomic.failWrite(output)
            throw e
        }
    }

    fun writeTextAtomic(file: File, text: String) {
        writeBytesAtomic(file, text.toByteArray(Charsets.UTF_8))
    }

    // ---------------- Cleanup helpers ----------------
    fun deleteSignatureFile(context: Context) {
        signatureFile(context).takeIf { it.exists() }?.delete()
    }

    fun deleteCredentialsTimestamp(context: Context) {
        timestampFile(context).takeIf { it.exists() }?.delete()
    }

    fun markCredentialsLocked(context: Context, reason: String = "unlock_failed") {
        val file = credentialsLockFile(context)
        if (!file.exists()) {
            writeTextAtomic(file, "$reason:${getCurrentUtcTimestamp()}")
        }
    }

    fun clearCredentialsLocked(context: Context) {
        credentialsLockFile(context).takeIf { it.exists() }?.delete()
    }

    fun isCredentialsLocked(context: Context): Boolean = credentialsLockFile(context).exists()

    // ---------------- Base64 ----------------
    fun base64Decode(encoded: String): ByteArray = Base64.decode(encoded, Base64.NO_WRAP)
    fun base64Encode(data: ByteArray): String = Base64.encodeToString(data, Base64.NO_WRAP)

    // ---------------- JSON hash (advisory only) ----------------
    /** SHA‑256 of the serialized JSON (order-sensitive). For advisory/integrity cues only. */
    fun computeJsonHash(json: JSONObject): String {
        val raw = json.toString().toByteArray()
        val digest = MessageDigest.getInstance("SHA-256").digest(raw)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
