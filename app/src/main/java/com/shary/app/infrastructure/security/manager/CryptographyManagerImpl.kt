package com.shary.app.infrastructure.security.manager

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.shary.app.core.domain.interfaces.security.CryptographyManager
import com.shary.app.core.domain.interfaces.security.DetachedVerifier
import com.shary.app.core.domain.interfaces.security.Ed25519Factory
import com.shary.app.core.domain.interfaces.states.Identity
import com.shary.app.core.domain.types.valueobjects.Sealed
import com.shary.app.infrastructure.security.box.AesGcmBox
import com.shary.app.infrastructure.security.messageCipher.AesGcmCipher
import com.shary.app.infrastructure.security.derivation.KeyDerivation
import com.shary.app.infrastructure.security.shared.keyExchange.X25519KeyPair
import com.shary.app.infrastructure.security.digitalSignature.Ed25519Signer
import com.shary.app.infrastructure.security.helper.SecurityUtils
import com.shary.app.infrastructure.security.helper.SecurityUtils.writeTextAtomic
import com.shary.app.infrastructure.security.local.LocalVault
import com.shary.app.utils.log.AppLogger
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec


/**
 * CryptographyManagerImpl
 *
 * Implementación del puerto de dominio `CryptographyManager`.
 *
 * - Deriva identidad (semillas y públicas) con `KeyDerivation`.
 * - Firma/verifica con Ed25519.
 * - Cifra/descifra credenciales JSON en almacenamiento local con **AES-GCM** usando `AesGcmCipher`
 *   y **clave derivada** estable desde (email, safePassword, appId) vía `KeyDerivation`.
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

    private var cachedUserEmail: String? = null
    private var cachedIdentity: Identity? = null

    // Tamaños fijos de AES-GCM
    private companion object {
        private const val IV_LEN = 12
        private const val TAG_LEN = 16
        private const val CREDENTIALS_WRAP_ALIAS = "com.shary.app.credentials.wrap.v1"
        private const val CREDENTIALS_WRAP_TRANSFORMATION = "AES/GCM/NoPadding"
        private val CREDENTIALS_WRAP_MAGIC = byteArrayOf('S'.code.toByte(), 'C'.code.toByte(), 'V'.code.toByte(), '3'.code.toByte())
    }

    // =====================================================================================
    // API de interfaz (dominio)
    // =====================================================================================

    /**
     * Inicializa/deriva las claves de usuario (en memoria) a partir de (email, safePassword).
     * Debe llamarse antes de `getSignPublicKey`, `signDetached`, etc.
     */
    override fun initializeKeysWithUser(context: Context, email: String, safePassword: String) {
        val id = deriveIdentity(email, safePassword.toCharArray(), getAppId())
        cachedUserEmail = email
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
        if (cachedIdentity == null || cachedUserEmail != email) {
            initializeKeysWithUser(context, email, safePassword)
        }
        val id = requireNotNull(cachedIdentity)
        val signPublic = id.getSignPublic()
        val kexPublic = id.getKexPublic()
        val json = JSONObject().apply {
            put("pub_sign_b64", Base64.encodeToString(signPublic, Base64.NO_WRAP))
            put("pub_kex_b64", Base64.encodeToString(kexPublic, Base64.NO_WRAP))
            put("key_id", publicKeyId(signPublic))
            put("ts", SecurityUtils.getCurrentUtcTimestamp())
            put("version", 2)
        }
        val f = SecurityUtils.signatureFile(context)
        if (!f.parentFile!!.exists()) f.parentFile!!.mkdirs()
        writeTextAtomic(f, json.toString())
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
     * Firma Ed25519 (detached). Rebuilds the signer from the derived seed.
     */
    override fun signDetached(message: ByteArray): ByteArray {
        val identity = requireNotNull(cachedIdentity) { "Keys not initialized. Call initializeKeysWithUser first." }
        //val signer = Ed25519Signer.fromSeed(identity.getSignSeed())
        AppLogger.debug("CryptographyManagerImpl", "event=sign_detached")
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
     * - AES-GCM (`AesGcmCipher`) con AAD = email (por defecto).
     *
     * Formato en disco (bytes):  iv(12) || body || tag(16)
     *
     * @param email      Dueño del blob; también se usa como AAD por defecto.
     * @param safePassword  Base64( hashPassword(plain, email) ) que usa la app.
     * @param json          Credenciales a cifrar.
     * @param aad           AAD opcional; si es null, se usa email bytes.
     * @return              email
     */
    override fun encryptCredentials(
        email: String,
        localKey: ByteArray,
        json: JSONObject,
        aad: ByteArray?
    ): ByteArray {
        val inner = localVault.encryptCredentials(email, localKey, json, aad)
        return wrapCredentialsBlob(inner)
    }

    override fun encryptCredentialsByDerivation(
        email: String,
        p: String,
        purpose: String,
        json: JSONObject,
        aad: ByteArray?
    ): ByteArray {
        val inner = localVault.encryptCredentialsByDerivation(email, p, json, aad)
        return wrapCredentialsBlob(inner)
    }

    /**
     * Descifra un blob de credenciales almacenadas localmente.
     * Debe ser simétrico con `encryptCredentialsJson`.
     *
     * @throws SecurityException si falla la autenticación (GCM tag) o el formato no es válido.
     */
    override fun decryptCredentials(
        email: String,
        localKey: ByteArray,
        encrypted: ByteArray,
        aad: ByteArray?
    ): JSONObject {
        val inner = unwrapCredentialsBlobOrLegacy(encrypted)
        return localVault.decryptCredentials(email, localKey, inner, aad)
    }

    override fun decryptCredentialsByDerivation(
        email: String,
        p: String,
        purpose: String,
        encrypted: ByteArray,
        aad: ByteArray?
    ): JSONObject {
        val inner = unwrapCredentialsBlobOrLegacy(encrypted)
        return localVault.decryptCredentialsByDerivation(email, p, inner, aad)
    }

    override fun isCredentialsBlobUsable(encrypted: ByteArray): Boolean {
        return runCatching {
            if (!isWrappedCredentialsBlob(encrypted)) {
                true
            } else {
                unwrapCredentialsBlobOrLegacy(encrypted)
                true
            }
        }.getOrDefault(false)
    }

    // =====================================================================================
    // API adicional (P2P / ECDH con Box) — útil fuera de almacenamiento local
    // =====================================================================================

    /**
     * Deriva identidad (semillas + públicas) desde (email, passwordChars, appId).
     * `passwordChars` puede ser el safePassword decodificado/transformado si así lo decidiste.
     */
    override fun deriveIdentity(email: String, password: CharArray, appId: String): Identity {
        val master = kd.masterSeed(email, password, appId)
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
        email: String,
        password: CharArray,
        appId: String,
        nonce: ByteArray,
        aad: ByteArray? = null
    ): Sealed {
        val master = kd.masterSeed(email, password, appId)
        val sess = kd.sessionSeed(master, nonce) // efímero determinista por nonce
        return box.seal(plain, myPrivate = sess, peerPublic = receiverKexPublic, aad = aad)
    }

    override fun sealTo(
        plain: ByteArray,
        receiverKexPublic: ByteArray,
        email: String,
        password: CharArray,
        appId: String,
        nonce: ByteArray,
        aad: ByteArray?
    ) = doSealTo(plain, receiverKexPublic, email, password, appId, nonce, aad)

    /**
     * Apertura ECDH (X25519 + AES-GCM) de un `Box.Sealed`.
     */
    private fun doOpenFrom(
        sealed: Sealed,
        senderEphPublicOrStatic: ByteArray,
        email: String,
        password: CharArray,
        appId: String,
        aad: ByteArray? = null
    ): ByteArray {
        val master = kd.masterSeed(email, password, appId)
        val kex    = kd.idKexSeed(master)
        return box.open(sealed, myPrivate = kex, peerPublic = senderEphPublicOrStatic, aad = aad)
    }
    override fun openFrom(
        sealedBox: Sealed,
        senderEphPublicOrStatic: ByteArray,
        email: String,
        password: CharArray,
        appId: String,
        aad: ByteArray?
    ) = doOpenFrom(sealedBox, senderEphPublicOrStatic, email, password, appId, aad)

    /** Clave pública X25519 del usuario (base64) derivada ad hoc. */
    fun deriveKexPublicKeyB64(email: String, password: CharArray): String {
        val id = deriveIdentity(email, password, getAppId())
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

    override fun deriveLocalKey(email: String, password: CharArray, purpose: String): ByteArray {
        return localVault.deriveLocalKey(email, password, purpose)
    }

    // =====================================================================================
    // Helpers internos
    // =====================================================================================

    override fun getAppId(): String = SecurityUtils.appId(context)

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

    private fun publicKeyId(publicKey: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(publicKey)
        return Base64.encodeToString(digest.copyOfRange(0, 16), Base64.NO_WRAP)
    }

    private fun wrapCredentialsBlob(inner: ByteArray): ByteArray {
        val key = getOrCreateCredentialsWrapKey()
        val cipher = Cipher.getInstance(CREDENTIALS_WRAP_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        require(iv.size == IV_LEN) { "Invalid Keystore IV length." }
        val wrapped = cipher.doFinal(inner)
        return ByteArray(CREDENTIALS_WRAP_MAGIC.size + iv.size + wrapped.size).apply {
            System.arraycopy(CREDENTIALS_WRAP_MAGIC, 0, this, 0, CREDENTIALS_WRAP_MAGIC.size)
            System.arraycopy(iv, 0, this, CREDENTIALS_WRAP_MAGIC.size, iv.size)
            System.arraycopy(wrapped, 0, this, CREDENTIALS_WRAP_MAGIC.size + iv.size, wrapped.size)
        }
    }

    private fun unwrapCredentialsBlobOrLegacy(blob: ByteArray): ByteArray {
        if (!isWrappedCredentialsBlob(blob)) return blob
        val ivStart = CREDENTIALS_WRAP_MAGIC.size
        val bodyStart = ivStart + IV_LEN
        val iv = blob.copyOfRange(ivStart, bodyStart)
        val wrapped = blob.copyOfRange(bodyStart, blob.size)
        return try {
            val key = getOrCreateCredentialsWrapKey()
            val cipher = Cipher.getInstance(CREDENTIALS_WRAP_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
            cipher.doFinal(wrapped)
        } catch (e: Throwable) {
            throw CredentialsWrapKeyException("Unable to unwrap credentials with Android Keystore key.", e)
        }
    }

    private fun isWrappedCredentialsBlob(blob: ByteArray): Boolean {
        if (blob.size <= CREDENTIALS_WRAP_MAGIC.size + IV_LEN + TAG_LEN) return false
        return CREDENTIALS_WRAP_MAGIC.indices.all { idx -> blob[idx] == CREDENTIALS_WRAP_MAGIC[idx] }
    }

    private fun getOrCreateCredentialsWrapKey(): SecretKey {
        return try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            (keyStore.getKey(CREDENTIALS_WRAP_ALIAS, null) as? SecretKey)?.let { return it }

            val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            val spec = KeyGenParameterSpec.Builder(
                CREDENTIALS_WRAP_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .setKeySize(256)
                .build()
            generator.init(spec)
            generator.generateKey()
        } catch (e: Throwable) {
            throw CredentialsWrapKeyException("Android Keystore key is unavailable for credentials wrapping.", e)
        }
    }
}
