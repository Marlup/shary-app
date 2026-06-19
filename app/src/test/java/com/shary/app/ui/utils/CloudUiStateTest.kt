package com.shary.app.ui.utils

import com.shary.app.core.domain.interfaces.states.CloudState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudUiStateTest {

    @Test
    fun cloudUiStatus_reportsLocalOnlyWhenOffline() {
        val status = cloudUiStatus(
            CloudState(
                isOnline = false,
                isUserValidated = true
            )
        )

        assertFalse(status.ready)
        assertEquals("Cloud offline", status.reason)
        assertEquals("Offline", status.label)
        assertEquals("Cloud actions stay local until connection returns.", status.detail)
        assertEquals("local-only", status.safetyLabel)
    }

    @Test
    fun cloudUiStatus_reportsBlockedWhenEmailIsNotVerified() {
        val status = cloudUiStatus(
            CloudState(
                isOnline = true,
                isUserValidated = false
            )
        )

        assertFalse(status.ready)
        assertEquals("Email not verified for cloud", status.reason)
        assertEquals("Verify email", status.label)
        assertEquals("Verify your email before syncing or reviewing cloud inbox.", status.detail)
        assertEquals("cloud-blocked", status.safetyLabel)
    }

    @Test
    fun cloudUiStatus_reportsReadyWhenOnlineAndVerified() {
        val status = cloudUiStatus(
            CloudState(
                isOnline = true,
                isUserValidated = true
            )
        )

        assertTrue(status.ready)
        assertEquals("", status.reason)
        assertEquals("Ready", status.label)
        assertEquals("Cloud inbox and secure relay are available.", status.detail)
        assertEquals("cloud-ready", status.safetyLabel)
    }
}
