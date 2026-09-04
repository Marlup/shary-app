package com.shary.app.infrastructure.security.manager

import android.content.Context
import com.shary.app.core.domain.interfaces.security.DetachedVerifier
import com.shary.app.core.domain.interfaces.security.Ed25519Factory
import com.shary.app.core.domain.interfaces.states.Identity
import com.shary.app.infrastructure.security.box.AesGcmBox
import com.shary.app.infrastructure.security.derivation.KeyDerivation
import com.shary.app.infrastructure.security.messageCipher.AesGcmCipher
import com.shary.app.infrastructure.security.local.LocalVault
import io.mockk.mockk
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class CryptographyManagerImplSessionTeardownTest {

    @Test
    fun clearSessionIdentityRemovesCachedUserAndKeyAccess() {
        val manager = CryptographyManagerImpl(
            kd = mockk<KeyDerivation>(relaxed = true),
            box = mockk<AesGcmBox>(relaxed = true),
            cipher = mockk<AesGcmCipher>(relaxed = true),
            factory = mockk<Ed25519Factory>(relaxed = true),
            localVault = mockk<LocalVault>(relaxed = true),
            verifier = mockk<DetachedVerifier>(relaxed = true),
            context = mockk<Context>(relaxed = true)
        )
        val identity = Identity(
            signPublic = byteArrayOf(1),
            kexPublic = byteArrayOf(2),
            signSeed = byteArrayOf(3, 5),
            kexSeed = byteArrayOf(4, 6)
        )
        manager.setPrivateField("cachedIdentity", identity)
        manager.setPrivateField("cachedUserEmail", "alice@example.com")

        manager.clearSessionIdentity()

        assertThrows(IllegalArgumentException::class.java) { manager.getSignPublic() }
        assertArrayEquals(byteArrayOf(0, 0), identity.getSignSeed())
        assertArrayEquals(byteArrayOf(0, 0), identity.getKexSeed())
        assertNull(manager.getPrivateField("cachedIdentity"))
        assertNull(manager.getPrivateField("cachedUserEmail"))
    }

    private fun Any.setPrivateField(name: String, value: Any?) {
        javaClass.getDeclaredField(name).apply {
            isAccessible = true
            set(this@setPrivateField, value)
        }
    }

    private fun Any.getPrivateField(name: String): Any? =
        javaClass.getDeclaredField(name).run {
            isAccessible = true
            get(this@getPrivateField)
        }
}
