package com.shary.app.infrastructure.services.cloud

import com.google.firebase.auth.FirebaseAuth
import com.shary.app.core.domain.interfaces.security.CryptographyManager
import com.shary.app.core.domain.interfaces.states.CloudState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudSessionResetTest {

    @Test
    fun resetClearsAllCloudIdentityAndWipesLocalKeyBytes() {
        val localKey = byteArrayOf(1, 2, 3)
        val state = CloudState(
            email = "alice@example.com",
            username = "alice",
            token = "token",
            fcmToken = "fcm",
            localKeys = mutableMapOf("value" to localKey),
            isOnline = true,
            isUserValidated = true
        )

        val cleared = state.clearSessionState()

        assertEquals(listOf<Byte>(0, 0, 0), localKey.toList())
        assertTrue(cleared.email.isBlank())
        assertTrue(cleared.username.isBlank())
        assertEquals("", cleared.token)
        assertEquals("", cleared.fcmToken)
        assertTrue(cleared.localKeys.isEmpty())
        assertFalse(cleared.isOnline)
        assertFalse(cleared.isUserValidated)
    }

    @Test
    fun cloudServiceClearsLocalStateBeforeFirebaseFailure() = runBlocking {
        val firebaseAuth = mockk<FirebaseAuth>()
        every { firebaseAuth.signOut() } throws IllegalStateException("firebase unavailable")
        val service = CloudServiceImpl(
            cryptographyManager = mockk<CryptographyManager>(relaxed = true),
            firebaseAuth = firebaseAuth
        )
        val localKey = byteArrayOf(4, 5, 6)
        service.cloudState.value = CloudState(
            email = "alice@example.com",
            username = "alice",
            token = "token",
            fcmToken = "fcm",
            localKeys = mutableMapOf("key" to localKey),
            isOnline = true,
            isUserValidated = true
        )

        val result = service.signOutCloud()

        assertTrue(result.isFailure)
        assertEquals(CloudState(), service.cloudState.value)
        assertEquals(listOf<Byte>(0, 0, 0), localKey.toList())
    }
}
