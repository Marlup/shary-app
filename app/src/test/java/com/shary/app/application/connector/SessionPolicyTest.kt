package com.shary.app.application.session

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionPolicyTest {
    @Test
    fun initial_navigation_waits_for_five_fields_unless_loading_is_complete() {
        assertFalse(isInitialFieldBatchReady(4, loadComplete = false))
        assertTrue(isInitialFieldBatchReady(5, loadComplete = false))
        assertTrue(isInitialFieldBatchReady(0, loadComplete = true))
        assertTrue(isInitialFieldBatchReady(3, loadComplete = true))
    }

    @Test
    fun background_session_expires_at_five_minutes() {
        val startedAt = 10_000L

        assertFalse(
            hasBackgroundSessionExpired(
                backgroundStartedAt = startedAt,
                now = startedAt + SESSION_BACKGROUND_TIMEOUT_MS - 1
            )
        )
        assertTrue(
            hasBackgroundSessionExpired(
                backgroundStartedAt = startedAt,
                now = startedAt + SESSION_BACKGROUND_TIMEOUT_MS
            )
        )
    }
}
