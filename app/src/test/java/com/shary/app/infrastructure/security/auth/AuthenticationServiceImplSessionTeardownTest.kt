package com.shary.app.infrastructure.security.auth

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.shary.app.core.domain.interfaces.persistance.CredentialsStore
import com.shary.app.core.domain.interfaces.security.CryptographyManager
import com.shary.app.core.domain.interfaces.services.CloudService
import com.shary.app.core.domain.interfaces.services.ClipboardService
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.types.valueobjects.Purpose
import com.shary.app.infrastructure.services.cache.SelectionCacheService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthenticationServiceImplSessionTeardownTest {

    @Test
    fun logoutClearsRuntimeStateEvenWhenExternalSignOutFails() = runBlocking {
        val crypto = mockk<CryptographyManager>(relaxed = true)
        val store = mockk<CredentialsStore>(relaxed = true)
        val cloud = mockk<CloudService>(relaxed = true)
        val firebaseAuth = mockk<FirebaseAuth>(relaxed = true)
        val clipboard = mockk<ClipboardService>(relaxed = true)
        val cache = SelectionCacheService().apply {
            cacheFields(listOf(FieldDomain.create(key = "secret", value = "account-a")))
        }
        val service = AuthenticationServiceImpl(
            crypto = crypto,
            store = store,
            cloud = cloud,
            cache = cache,
            firebaseAuth = firebaseAuth,
            clipboardService = clipboard
        )
        val localKey = byteArrayOf(1, 2, 3)
        service.setSafePassword("derived-secret")
        service.addLocalKeyByPurpose(Purpose.Key, localKey)
        coEvery { cloud.signOutCloud() } returns Result.failure(IllegalStateException("offline"))
        every { crypto.clearSessionIdentity() } throws IllegalStateException("crypto unavailable")
        every { firebaseAuth.signOut() } throws IllegalStateException("firebase unavailable")

        service.logoutForRelogin()

        assertTrue(service.state.value.email.isBlank())
        assertTrue(service.state.value.safePassword.isBlank())
        assertTrue(service.state.value.localKeys.isEmpty())
        assertEquals(listOf<Byte>(0, 0, 0), localKey.toList())
        assertTrue(cache.getFields().isEmpty())
        verify(exactly = 1) { crypto.clearSessionIdentity() }
        verify(exactly = 1) { clipboard.clearOwnedClip() }
        coVerify(exactly = 1) { cloud.signOutCloud() }
        verify(exactly = 1) { firebaseAuth.signOut() }
        verify(exactly = 0) { store.deleteCredentials(any<Context>()) }
    }

    @Test
    fun failedSignInIsClearedBeforeAndAfterTheAttempt() = runBlocking {
        val crypto = mockk<CryptographyManager>(relaxed = true)
        val store = mockk<CredentialsStore>(relaxed = true)
        val cloud = mockk<CloudService>(relaxed = true)
        val firebaseAuth = mockk<FirebaseAuth>(relaxed = true)
        val clipboard = mockk<ClipboardService>(relaxed = true)
        val cache = SelectionCacheService().apply {
            cacheFields(listOf(FieldDomain.create(key = "secret", value = "account-a")))
        }
        val service = AuthenticationServiceImpl(
            crypto = crypto,
            store = store,
            cloud = cloud,
            cache = cache,
            firebaseAuth = firebaseAuth,
            clipboardService = clipboard
        )
        coEvery { cloud.signOutCloud() } returns Result.success(Unit)
        every { crypto.hashPassword(any(), any()) } throws IllegalArgumentException("bad login")

        val result = service.signIn(
            context = mockk(relaxed = true),
            email = "alice@example.com",
            password = "wrong"
        )

        assertTrue(result.isFailure)
        assertTrue(cache.getFields().isEmpty())
        verify(exactly = 2) { crypto.clearSessionIdentity() }
        verify(exactly = 2) { clipboard.clearOwnedClip() }
        coVerify(exactly = 2) { cloud.signOutCloud() }
        verify(exactly = 2) { firebaseAuth.signOut() }
    }
}
