package com.shary.app.core.domain.interfaces.security

import android.content.Context
import com.shary.app.core.domain.interfaces.states.Identity
import com.shary.app.core.domain.security.Box
import com.shary.app.core.domain.types.valueobjects.Purpose
import org.json.JSONObject

interface CryptographyManager {
    /**
     * Initialize/load local key material for a user bound to (username + safePassword).
     * Must be called before any operation that depends on user-scoped keys.
     */
    fun initializeKeysWithUser(context: Context, username: String, safePassword: String)

    /**
     * Persist a signature/identity file for the user. Implementation decides the format.
     */
    fun saveSignature(context: Context, username: String, email: String, safePassword: String)

    /**
     * Deterministic hash of a password with a salt (e.g., username) producing the "safePassword" seed.
     * The app uses base64(hashPassword(password, username)) as its stored/derived safe value.
     */
    fun hashPassword(password: String, salt: String): ByteArray

    /** Public keys and signatures for Ed25519/X25519 flows. */
    fun getSignPublic(): ByteArray
    fun getKexPublic(): ByteArray
    fun signDetached(message: ByteArray): ByteArray
    fun verifyDetached(message: ByteArray, signature: ByteArray, publicKey: ByteArray): Boolean
    fun deriveIdentity(username: String, password: CharArray, appId: String): Identity
    fun openFrom(
        sealed: Box.Sealed,
        senderEphPublicOrStatic: ByteArray,
        username: String,
        password: CharArray,
        appId: String,
        aad: ByteArray? = null
    ): ByteArray

    fun sealTo(
        plain: ByteArray,
        receiverKexPublic: ByteArray,
        username: String,
        password: CharArray,
        appId: String,
        nonce: ByteArray,
        aad: ByteArray? = null
    ): Box.Sealed

    /**
     * Encrypt a JSON credentials object using the manager’s **official** storage scheme.
     * This should internally handle KDF (from username + safePassword), AEAD mode, IV/nonce,
     * and (optionally) AAD binding (e.g., username).
     *
     * @param username       Owner username (used for AAD/binding and/or salt derivation).
     * @param localKey   Base64-encoded result of hashPassword(password, username) (the app's “safe”).
     * @param json           JSON to encrypt.
     * @param aad            Optional AAD to bind; default: username bytes. Can be ignored if impl does not use AAD.
     * @return Opaque ciphertext blob ready to write to disk.
     */
    fun encryptCredentials(
        username: String,
        localKey: ByteArray,
        json: JSONObject,
        aad: ByteArray? = username.toByteArray()
    ): ByteArray

    fun encryptCredentialsByDerivation(
        username: String,
        p: String,
        purpose: String,
        json: JSONObject,
        aad: ByteArray? = username.toByteArray()
    ): ByteArray

    /**
     * Decrypt a credentials blob produced by [encryptCredentials].
     * Must support migration/legacy decoding if you still have older blobs.
     *
     * @param username       Owner username (used for AAD/binding and/or salt derivation).
     * @param localKey   Base64-encoded result of hashPassword(password, username).
     * @param encrypted      Ciphertext blob read from disk.
     * @param aad            Optional AAD used on encryption; default matches username.
     * @return Decrypted credentials JSON.
     * @throws SecurityException if authentication/tag check fails or format is invalid.
     */
    fun decryptCredentials(
        username: String,
        localKey: ByteArray,
        encrypted: ByteArray,
        aad: ByteArray? = username.toByteArray()
    ): JSONObject

    fun decryptCredentialsByDerivation(
        username: String,
        p: String,
        purpose: String,
        encrypted: ByteArray,
        aad: ByteArray? = username.toByteArray()
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
        username: String,
        password: CharArray,
        purpose: String = Purpose.Key.code
    ): ByteArray
}
