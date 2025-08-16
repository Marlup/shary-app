package com.shary.app.infrastructure.security.manager

import android.content.Context
import android.util.Base64
import com.shary.app.core.domain.interfaces.security.CryptographyManager
import com.shary.app.core.domain.interfaces.security.DetachedVerifier
import com.shary.app.core.domain.interfaces.security.Ed25519Factory
import com.shary.app.core.domain.interfaces.states.Identity
import com.shary.app.core.domain.security.Box
import com.shary.app.infrastructure.security.box.AesGcmBox
import com.shary.app.infrastructure.security.cipher.AesGcmCipher
import com.shary.app.infrastructure.security.derivation.KeyDerivation
import com.shary.app.infrastructure.security.kex.X25519KeyPair
import com.shary.app.infrastructure.security.sign.Ed25519Signer
import com.shary.app.infrastructure.security.helper.SecurityUtils
import com.shary.app.infrastructure.security.local.LocalVault
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.spec.X509EncodedKeySpec


/**
 * CryptographyManagerImpl
 *
 * Implementación del puerto de dominio `CryptographyManager`.
 *
 * - Deriva identidad (semillas y públicas) con `KeyDerivation`.
 * - Firma/verifica con Ed25519.
 * - Cifra/descifra credenciales JSON en almacenamiento local con **AES-GCM** usando `AesGcmCipher`
 *   y **clave derivada** estable desde (username, safePassword, appId) vía `KeyDerivation`.
 * - Opcionalmente, realiza sellado ECDH (X25519 + AES-GCM) con `AesGcmBox` para P2P o C/S.
 */
class CryptographyManagerImpl(
    private val kd: KeyDerivation,
    private val box: AesGcmBox,           // Para ECDH seal/open (P2P, no local storage)
    private val cipher: AesGcmCipher,     // Para cifrado simétrico local (credenciales)
    private val factory: Ed25519Factory,
    private val localVault: LocalVault,
    private val verifier: DetachedVerifier,
    private val context: Context
) : CryptographyManager {

    private var cachedUser: String? = null
    private var cachedIdentity: Identity? = null

    // Tamaños fijos de AES-GCM
    private companion object {
        private const val IV_LEN = 12
        private const val TAG_LEN = 16
    }

    // =====================================================================================
    // API de interfaz (dominio)
    // =====================================================================================

    /**
     * Inicializa/deriva las claves de usuario (en memoria) a partir de (username, safePassword).
     * Debe llamarse antes de `getSignPublicKey`, `signDetached`, etc.
     */
    override fun initializeKeysWithUser(context: Context, username: String, safePassword: String) {
        val id = deriveIdentity(username, safePassword.toCharArray(), getAppId())
        cachedUser = username
        cachedIdentity = id
    }

    /**
     * Persiste un fichero JSON de identidad con las claves públicas (no secretas):
     * {
     *   "username": "...",
     *   "email": "...",
     *   "pub_sign_b64": "...",
     *   "pub_kex_b64": "...",
     *   "ts": 1234567890,
     *   "app_id": "com.tu.app"
     * }
     */
    override fun saveSignature(context: Context, username: String, email: String, safePassword: String) {
        if (cachedIdentity == null || cachedUser != username) {
            initializeKeysWithUser(context, username, safePassword)
        }
        val id = requireNotNull(cachedIdentity)
        val json = JSONObject().apply {
            put("username", username)
            put("email", email)
            put("pub_sign_b64", Base64.encodeToString(id.getSignPublic(), Base64.NO_WRAP))
            put("pub_kex_b64", Base64.encodeToString(id.getKexPublic(), Base64.NO_WRAP))
            put("ts", SecurityUtils.getCurrentUtcTimestamp())
            put("app_id", getAppId())
        }
        val f = SecurityUtils.signatureFile(context)
        if (!f.parentFile.exists()) f.parentFile.mkdirs()
        f.writeText(json.toString())
    }

    /**
     * Devuelve el hash determinista (bytes) de password+salt que la app codifica en base64
     * para construir el `safePassword`.
     *
     * Si `KeyDerivation` expone una primitiva de hash específica, úsala aquí.
     * En su defecto, se usa SHA-256(password ":" salt).
     */
    override fun hashPassword(password: String, salt: String): ByteArray {
        return runCatching {
            // Si existe kd.passwordHash(...), úsalo:
            // kd.passwordHash(password.toCharArray(), salt.toByteArray(StandardCharsets.UTF_8))
            throw UnsupportedOperationException("fallback")
        }.getOrElse {
            val md = MessageDigest.getInstance("SHA-256")
            md.update(password.toByteArray(StandardCharsets.UTF_8))
            md.update(':'.code.toByte())
            md.update(salt.toByteArray(StandardCharsets.UTF_8))
            md.digest()
        }
    }

    /** Clave pública Ed25519 del usuario cargado con initializeKeysWithUser. */
    override fun getSignPublic(): ByteArray =
        requireNotNull(cachedIdentity) {
            "Keys not initialized. Call initializeKeysWithUser first."
        }.getSignPublic()

    /** Clave pública X25519 del usuario cargado con initializeKeysWithUser. */
    override fun getKexPublic(): ByteArray =
        requireNotNull(cachedIdentity) {
            "Keys not initialized. Call initializeKeysWithUser first."
        }.getKexPublic()

    /**
     * Firma Ed25519 (detached). Se recrea el signer desde la seed derivada.
     */
    override fun signDetached(message: ByteArray): ByteArray {
        val identity = requireNotNull(cachedIdentity) { "Keys not initialized. Call initializeKeysWithUser first." }
        //val signer = Ed25519Signer.fromSeed(identity.getSignSeed())
        val signer = factory.signerFromSeed(identity.getSignSeed()) // short‑lived
        return signer.sign(message)
    }

    /**
     * Verifica firma Ed25519 (detached) con clave pública dada.
     */
    override fun verifyDetached(
        message: ByteArray,
        signature: ByteArray,
        publicKey: ByteArray
    ): Boolean = verifier.verify(message, signature, publicKey)

    /**
     * Cifra un JSON de credenciales para almacenamiento local usando:
     * - KDF oficial (`KeyDerivation`) → `localStorageKey(master)` (o `storageKey(master)`).
     * - AES-GCM (`AesGcmCipher`) con AAD = username (por defecto).
     *
     * Formato en disco (bytes):  iv(12) || body || tag(16)
     *
     * @param username      Dueño del blob; también se usa como AAD por defecto.
     * @param safePassword  Base64( hashPassword(plain, username) ) que usa la app.
     * @param json          Credenciales a cifrar.
     * @param aad           AAD opcional; si es null, se usa username bytes.
     * @return              Blob opaco listo para persistir.
     */
    override fun encryptCredentialsJson(
        username: String,
        safePassword: String,
        json: JSONObject,
        aad: ByteArray?
    ): ByteArray {
        return localVault.encryptCredentialsJson(username, safePassword, json, aad)
    }

    /**
     * Descifra un blob de credenciales almacenadas localmente.
     * Debe ser simétrico con `encryptCredentialsJson`.
     *
     * @throws SecurityException si falla la autenticación (GCM tag) o el formato no es válido.
     */
    override fun decryptCredentialsJson(
        username: String,
        safePassword: String,
        encrypted: ByteArray,
        aad: ByteArray?
    ): JSONObject {
        return localVault.decryptCredentialsJson(username, safePassword, encrypted, aad)
    }

    // =====================================================================================
    // API adicional (P2P / ECDH con Box) — útil fuera de almacenamiento local
    // =====================================================================================

    /**
     * Deriva identidad (semillas + públicas) desde (username, passwordChars, appId).
     * `passwordChars` puede ser el safePassword decodificado/transformado si así lo decidiste.
     */
    override fun deriveIdentity(username: String, password: CharArray, appId: String): Identity {
        val master = kd.masterSeed(username, password, appId)
        val signSeed = kd.idSignSeed(master)
        val kexSeed  = kd.idKexSeed(master)
        val signer   = Ed25519Signer.fromSeed(signSeed)
        val kex      = X25519KeyPair.fromSeed(kexSeed)

        return Identity(
            signer.getPublicKey().copyOf(),
            kex.publicEncoded().copyOf(),
            signSeed,
            kexSeed
        )
    }

    /**
     * Sellado ECDH (X25519 + AES-GCM) para envío a un receptor.
     * Usa `sessionSeed(master, nonce)` para una clave efímera determinista por nonce.
     */
    private fun doSealTo(
        plain: ByteArray,
        receiverKexPublic: ByteArray,
        username: String,
        password: CharArray,
        appId: String,
        nonce: ByteArray,
        aad: ByteArray? = null
    ): Box.Sealed {
        val master = kd.masterSeed(username, password, appId)
        val sess = kd.sessionSeed(master, nonce) // efímero determinista por nonce
        return box.seal(plain, myPrivate = sess, peerPublic = receiverKexPublic, aad = aad)
    }

    override fun sealTo(
        plain: ByteArray,
        receiverKexPublic: ByteArray,
        username: String,
        password: CharArray,
        appId: String,
        nonce: ByteArray,
        aad: ByteArray?
    ) = doSealTo(plain, receiverKexPublic, username, password, appId, nonce, aad)

    /**
     * Apertura ECDH (X25519 + AES-GCM) de un `Box.Sealed`.
     */
    private fun doOpenFrom(
        sealed: Box.Sealed,
        senderEphPublicOrStatic: ByteArray,
        username: String,
        password: CharArray,
        appId: String,
        aad: ByteArray? = null
    ): ByteArray {
        val master = kd.masterSeed(username, password, appId)
        val kex    = kd.idKexSeed(master)
        return box.open(sealed, myPrivate = kex, peerPublic = senderEphPublicOrStatic, aad = aad)
    }
    override fun openFrom(
        sealed: Box.Sealed,
        senderEphPublicOrStatic: ByteArray,
        username: String,
        password: CharArray,
        appId: String,
        aad: ByteArray?
    ) = doOpenFrom(sealed, senderEphPublicOrStatic, username, password, appId, aad)

    /** Clave pública X25519 del usuario (base64) derivada ad hoc. */
    fun deriveKexPublicKeyB64(username: String, password: CharArray): String {
        val id = deriveIdentity(username, password, getAppId())
        return Base64.encodeToString(id.getKexPublic(), Base64.NO_WRAP)
    }

    override fun encryptWithPeerPublic(
        plain: ByteArray,
        peerKexPublic: ByteArray,
        aad: ByteArray?
    ): ByteArray {
        val id = requireNotNull(cachedIdentity) { "Keys not initialized" }

        // Build my static X25519 key pair from stored seed
        val myKex = X25519KeyPair.fromSeed(id.getKexSeed())

        // ECDH → 32-byte shared key
        val sharedKey = myKex.ecdh(peerKexPublic)

        // AES-GCM encrypt
        val (iv, body, tag) = cipher.encrypt(sharedKey, plain, aad)

        // Pack into one byte array: iv || body || tag
        return ByteArray(iv.size + body.size + tag.size).apply {
            System.arraycopy(iv, 0, this, 0, iv.size)
            System.arraycopy(body, 0, this, iv.size, body.size)
            System.arraycopy(tag, 0, this, iv.size + body.size, tag.size)
        }
    }


    override fun decryptFromPeerPublic(
        encrypted: ByteArray,
        peerKexPublic: ByteArray,
        aad: ByteArray?
    ): ByteArray {
        val id = requireNotNull(cachedIdentity) { "Keys not initialized" }

        // Split iv/body/tag
        val iv = encrypted.copyOfRange(0, 12)
        val tag = encrypted.copyOfRange(encrypted.size - 16, encrypted.size)
        val body = encrypted.copyOfRange(12, encrypted.size - 16)

        // My static key pair from seed
        val myKex = X25519KeyPair.fromSeed(id.getKexSeed())

        // ECDH shared key
        val sharedKey = myKex.ecdh(peerKexPublic)

        // AES-GCM decrypt
        return cipher.decrypt(sharedKey, iv, body, tag, aad)
    }


    // =====================================================================================
    // Helpers internos
    // =====================================================================================

    override fun getAppId(): String = SecurityUtils.appId(context)

    /**
     * Deriva la clave simétrica de almacenamiento local desde (username, safePassword, appId).
     * Convención típica:
     *   master = kd.masterSeed(username, safePassword.toCharArray(), getAppId(context))
     *   key    = kd.localStorageKey(master)   // o kd.storageKey(master)
     */
    fun localStorageKey(username: String, safePassword: String): ByteArray {
        //val master = kd.masterSeed(username, safePassword.toCharArray(), getAppId(context))
        //return runCatching { kd.localStorageKey(master) }
        //    .getOrElse { kd.storageKey(master) } // usa el nombre que tengas en tu implementación
        return localVault.deriveLocalKey(username, safePassword.toCharArray())
    }

    /** Empaqueta el resultado de AES-GCM como: iv(12) || body || tag(16) */
    private fun pack(iv: ByteArray, body: ByteArray, tag: ByteArray): ByteArray {
        require(iv.size == IV_LEN) { "IV inválido: se esperaban $IV_LEN bytes." }
        require(tag.size == TAG_LEN) { "TAG inválida: se esperaban $TAG_LEN bytes." }
        return ByteArray(IV_LEN + body.size + TAG_LEN).apply {
            System.arraycopy(iv, 0, this, 0, IV_LEN)
            System.arraycopy(body, 0, this, IV_LEN, body.size)
            System.arraycopy(tag, 0, this, IV_LEN + body.size, TAG_LEN)
        }
    }

    /** Desempaqueta iv/body/tag del blob: iv(12) || body || tag(16) */
    private fun unpack(encrypted: ByteArray): Triple<ByteArray, ByteArray, ByteArray> {
        require(encrypted.size >= IV_LEN + TAG_LEN) { "Blob cifrado demasiado corto." }
        val iv   = encrypted.copyOfRange(0, IV_LEN)
        val tag  = encrypted.copyOfRange(encrypted.size - TAG_LEN, encrypted.size)
        val body = encrypted.copyOfRange(IV_LEN, encrypted.size - TAG_LEN)
        return Triple(iv, body, tag)
    }
}
