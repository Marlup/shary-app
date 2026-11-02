package com.shary.app.infrastructure.security.backup

import com.shary.app.infrastructure.security.messageCipher.AesGcmCipher
import com.shary.app.infrastructure.security.derivation.kdf.Pbkdf2Kdf
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class VaultBackupCipherTest {

    private val cipher = VaultBackupCipher(Pbkdf2Kdf(), AesGcmCipher())
    private val passphrase = "correct horse battery staple".toCharArray()

    @Test
    fun roundTripPreservesPayload() {
        val plaintext = """{"format":"shary-vault","version":1,"data":"secret"}"""
            .toByteArray(Charsets.UTF_8)

        val container = cipher.encrypt(plaintext, passphrase)
        val decrypted = cipher.decrypt(container, passphrase)

        assertFalse(container.contentEquals(plaintext))
        assertArrayEquals(plaintext, decrypted)
    }

    @Test
    fun wrongPassphraseIsRejected() {
        val container = cipher.encrypt("secret".toByteArray(), passphrase)
        expectThrows<VaultBackupWrongPassphraseException> {
            cipher.decrypt(container, "wrong passphrase".toCharArray())
        }
    }

    @Test
    fun tamperedCiphertextIsRejected() {
        val container = cipher.encrypt("secret".toByteArray(), passphrase)
        container[container.lastIndex] = (container.last().toInt() xor 0x01).toByte()

        expectThrows<VaultBackupWrongPassphraseException> {
            cipher.decrypt(container, passphrase)
        }
    }

    @Test
    fun unsupportedVersionIsRejected() {
        val container = cipher.encrypt("secret".toByteArray(), passphrase)
        container[8] = 2

        expectThrows<VaultBackupFormatException> {
            cipher.decrypt(container, passphrase)
        }
    }

    private inline fun <reified T : Throwable> expectThrows(block: () -> Unit) {
        try {
            block()
            fail("Expected ${T::class.java.simpleName}")
        } catch (error: Throwable) {
            assertTrue(
                "Expected ${T::class.java.simpleName}, got ${error::class.java.simpleName}",
                error is T
            )
        }
    }
}
