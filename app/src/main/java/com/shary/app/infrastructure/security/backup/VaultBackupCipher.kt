package com.shary.app.infrastructure.security.backup

import com.shary.app.core.domain.security.Kdf
import com.shary.app.infrastructure.security.messageCipher.AesGcmCipher
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.inject.Inject
import javax.inject.Singleton

class VaultBackupFormatException(message: String) : Exception(message)

class VaultBackupWrongPassphraseException :
    Exception("Wrong passphrase or corrupted vault backup.")

/**
 * Encrypts and decrypts the portable vault backup container.
 *
 * Container layout (big-endian):
 *   magic (8) | version (1) | iterations (4) | saltLen (1) | salt |
 *   ivLen (1) | iv | ciphertextLen (4) | ciphertext body | GCM tag (16)
 *
 * The header is authenticated as AAD, so version/iteration/salt tampering is
 * detected before the payload is used.
 */
@Singleton
class VaultBackupCipher @Inject constructor(
    private val kdf: Kdf,
    private val cipher: AesGcmCipher
) {
    companion object {
        private val MAGIC = "SHARYVB1".toByteArray(Charsets.US_ASCII)
        private const val VERSION = 1
        private const val ITERATIONS = 600_000
        private const val SALT_LEN = 16
        private const val IV_LEN = 12
        private const val TAG_LEN = 16
        private const val MAX_CONTAINER_BYTES = 64 * 1024 * 1024
    }

    private val rng = SecureRandom()

    fun encrypt(plaintext: ByteArray, passphrase: CharArray): ByteArray {
        require(passphrase.isNotEmpty()) { "Passphrase must not be empty." }
        require(plaintext.size <= MAX_CONTAINER_BYTES) { "Vault backup is too large." }

        val salt = ByteArray(SALT_LEN).also(rng::nextBytes)
        val key = kdf.derive(passphrase, salt, 32, ITERATIONS)
        try {
            val aad = headerAad(salt)
            val (iv, body, tag) = cipher.encrypt(key, plaintext, aad)

            val total = MAGIC.size + 1 + 4 + 1 + salt.size + 1 + iv.size + 4 +
                body.size + tag.size
            return ByteBuffer.allocate(total)
                .put(MAGIC)
                .put(VERSION.toByte())
                .putInt(ITERATIONS)
                .put(SALT_LEN.toByte())
                .put(salt)
                .put(IV_LEN.toByte())
                .put(iv)
                .putInt(body.size + tag.size)
                .put(body)
                .put(tag)
                .array()
        } finally {
            key.fill(0)
        }
    }

    fun decrypt(container: ByteArray, passphrase: CharArray): ByteArray {
        require(passphrase.isNotEmpty()) { "Passphrase must not be empty." }
        if (container.size > MAX_CONTAINER_BYTES) {
            throw VaultBackupFormatException("Vault backup is too large.")
        }
        if (container.size < MAGIC.size + 1 + 4 + 1 + SALT_LEN + 1 + IV_LEN + 4 + TAG_LEN) {
            throw VaultBackupFormatException("Vault backup is truncated.")
        }

        val buffer = ByteBuffer.wrap(container)
        val magic = ByteArray(MAGIC.size).also(buffer::get)
        if (!magic.contentEquals(MAGIC)) {
            throw VaultBackupFormatException("Not a Shary vault backup.")
        }

        val version = buffer.get().toInt() and 0xFF
        if (version != VERSION) {
            throw VaultBackupFormatException("Unsupported vault backup version: $version.")
        }

        val iterations = buffer.int
        if (iterations != ITERATIONS) {
            throw VaultBackupFormatException("Unsupported KDF iteration count.")
        }

        val saltLength = buffer.get().toInt() and 0xFF
        if (saltLength != SALT_LEN || buffer.remaining() < saltLength) {
            throw VaultBackupFormatException("Invalid salt.")
        }
        val salt = ByteArray(saltLength).also(buffer::get)

        val ivLength = buffer.get().toInt() and 0xFF
        if (ivLength != IV_LEN || buffer.remaining() < ivLength) {
            throw VaultBackupFormatException("Invalid IV.")
        }
        val iv = ByteArray(ivLength).also(buffer::get)

        if (buffer.remaining() < 4) {
            throw VaultBackupFormatException("Vault backup is truncated.")
        }
        val ciphertextLength = buffer.int
        if (ciphertextLength < TAG_LEN || ciphertextLength > buffer.remaining()) {
            throw VaultBackupFormatException("Invalid ciphertext length.")
        }

        val ciphertext = ByteArray(ciphertextLength).also(buffer::get)
        if (buffer.hasRemaining()) {
            throw VaultBackupFormatException("Unexpected trailing data.")
        }

        val body = ciphertext.copyOfRange(0, ciphertext.size - TAG_LEN)
        val tag = ciphertext.copyOfRange(ciphertext.size - TAG_LEN, ciphertext.size)
        val key = kdf.derive(passphrase, salt, 32, iterations)
        try {
            return cipher.decrypt(key, iv, body, tag, headerAad(salt))
        } catch (error: AEADBadTagException) {
            throw VaultBackupWrongPassphraseException()
        } catch (error: VaultBackupWrongPassphraseException) {
            throw error
        } catch (error: Exception) {
            throw VaultBackupFormatException(
                "Vault backup could not be decrypted: ${error.message ?: "invalid format"}."
            )
        } finally {
            key.fill(0)
        }
    }

    private fun headerAad(salt: ByteArray): ByteArray =
        ByteBuffer.allocate(MAGIC.size + 1 + 4 + 1 + salt.size)
            .put(MAGIC)
            .put(VERSION.toByte())
            .putInt(ITERATIONS)
            .put(SALT_LEN.toByte())
            .put(salt)
            .array()
}
