package com.shary.app.core.domain.interfaces.security

import android.content.Context
import com.shary.app.core.domain.interfaces.states.Identity
import com.shary.app.core.domain.types.valueobjects.Purpose
import com.shary.app.core.domain.types.valueobjects.Sealed
import org.json.JSONObject

interface CryptographyManager {
    /**
     * Initialize/load local key material for a user bound to (email + safePassword).
     * Must be called before any operation that depends on user-scoped keys.
     */
    fun initializeKeysWithUser(context: Context, email: String, safePassword: String)

    /**
     * Persist a signature/identity file for the user. Implementation decides the format.
     */
    fun saveSignature(context: Context, username: String, email: String, safePassword: String)

    /**
     * Deterministic hash of a password with a salt (e.g., email) producing the "safePassword" seed.
     * The app uses base64(hashPassword(password, email)) as its stored/derived safe value.
     */
    fun hashPassword(password: String, salt: String): ByteArray

    /** Public keys and signatures for Ed25519/X25519 flows. */
    fun getSignPublic(): ByteArray
    fun getKexPublic(): ByteArray
    fun signDetached(message: ByteArray): ByteArray
    fun verifyDetached(message: ByteArray, signature: ByteArray, publicKey: ByteArray): Boolean
    fun deriveIdentity(email: String, password: CharArray, appId: String): Identity
    fun openFrom(
        sealedBox: Sealed,
        senderEphPublicOrStatic: ByteArray,
        email: String,
        password: CharArray,
        appId: String,
        aad: ByteArray? = null
    ): ByteArray

    fun sealTo(
        plain: ByteArray,
        receiverKexPublic: ByteArray,
        email: String,
        password: CharArray,
        appId: String,
        nonce: ByteArray,
        aad: ByteArray? = null
    ): Sealed

    /**
     * Encrypt a JSON credentials object using the manager’s **official** storage scheme.
     * This should internally handle KDF (from username + safePassword), AEAD mode, IV/nonce,
     * and (optionally) AAD binding (e.g., username).
     *
     * @param email       Owner username (used for AAD/binding and/or salt derivation).
     * @param localKey   Base64-encoded result of hashPassword(password, username) (the app's “safe”).
     * @param json           JSON to encrypt.
     * @param aad            Optional AAD to bind; default: username bytes. Can be ignored if impl does not use AAD.
     * @return Opaque ciphertext blob ready to write to disk.
     */
    fun encryptCredentials(
        email: String,
        localKey: ByteArray,
        json: JSONObject,
        aad: ByteArray? = email.toByteArray()
    ): ByteArray

    fun encryptCredentialsByDerivation(
        email: String,
        p: String,
        purpose: String,
        json: JSONObject,
        aad: ByteArray? = email.toByteArray()
    ): ByteArray

    /**
     * Decrypt a credentials blob produced by [encryptCredentials].
     * Must support migration/legacy decoding if you still have older blobs.
     *
     * @param email       Owner username (used for AAD/binding and/or salt derivation).
     * @param localKey   Base64-encoded result of hashPassword(password, username).
     * @param encrypted      Ciphertext blob read from disk.
     * @param aad            Optional AAD used on encryption; default matches username.
     * @return Decrypted credentials JSON.
     * @throws SecurityException if authentication/tag check fails or format is invalid.
     */
    fun decryptCredentials(
        email: String,
        localKey: ByteArray,
        encrypted: ByteArray,
        aad: ByteArray? = email.toByteArray()
    ): JSONObject

    fun decryptCredentialsByDerivation(
        email: String,
        p: String,
        purpose: String,
        encrypted: ByteArray,
        aad: ByteArray? = email.toByteArray()
    ): JSONObject

    fun getAppId(): String

    fun encryptWithPeerPublic(
        plain: ByteArray,
        peerKexPublic: ByteArray,
        aad: ByteArray? = null
    ): ByteArray

    fun decryptFromPeerPublic(
        encrypted: ByteArray,
        peerKexPublic: ByteArray,
        aad: ByteArray? = null
    ): ByteArray

    fun deriveLocalKey(
        email: String,
        password: CharArray,
        purpose: String = Purpose.Key.code
    ): ByteArray
}
