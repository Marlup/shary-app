package com.shary.app.infrastructure.services.clipboard

import androidx.lifecycle.LifecycleOwner
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClipboardCleanupCoordinatorTest {

    @Test
    fun scheduledClearSurvivesTheCallingScreenAndClearsTheOwnedClip() {
        val gateway = FakeClipboardGateway()
        val scheduler = FakeClipboardScheduler()
        val coordinator = ClipboardCleanupCoordinator(gateway, scheduler)

        coordinator.copyText("field_value", "secret", sensitive = true, clearAfterSeconds = 30)
        scheduler.runLatest()

        assertTrue(gateway.lastSensitive)
        assertNull(gateway.currentToken)
        assertEquals(1, gateway.clearCount)
    }

    @Test
    fun aNewCopyCancelsTheOldExpiryAndNeverClearsTheReplacementEarly() {
        val gateway = FakeClipboardGateway()
        val scheduler = FakeClipboardScheduler()
        val coordinator = ClipboardCleanupCoordinator(gateway, scheduler)

        coordinator.copyText("field_value", "first", sensitive = false, clearAfterSeconds = 30)
        val firstTask = scheduler.latest
        coordinator.copyText("field_value", "second", sensitive = false, clearAfterSeconds = 30)

        firstTask?.runEvenIfCancelled()
        assertEquals("second", gateway.currentText)
        assertEquals(0, gateway.clearCount)

        scheduler.runLatest()
        assertNull(gateway.currentText)
        assertEquals(1, gateway.clearCount)
    }

    @Test
    fun backgroundingClearsImmediatelyAndDoesNotClearAnExternalReplacement() {
        val gateway = FakeClipboardGateway()
        val scheduler = FakeClipboardScheduler()
        val coordinator = ClipboardCleanupCoordinator(gateway, scheduler)
        val owner = mockk<LifecycleOwner>(relaxed = true)

        coordinator.copyText("contact_email", "alice@example.com", false, 30)
        gateway.replaceExternally("external")
        coordinator.onStop(owner)

        assertEquals("external", gateway.currentText)
        assertEquals(0, gateway.clearCount)
        assertTrue(scheduler.latest?.cancelled == true)
    }

    @Test
    fun disabledExpiryLeavesNoScheduledTaskButSessionClearStillWorks() {
        val gateway = FakeClipboardGateway()
        val scheduler = FakeClipboardScheduler()
        val coordinator = ClipboardCleanupCoordinator(gateway, scheduler)

        coordinator.copyText("field_value", "value", false, 0)
        assertNull(scheduler.latest)

        coordinator.clearOwnedClip()
        assertNull(gateway.currentText)
        assertEquals(1, gateway.clearCount)
    }

    private class FakeClipboardGateway : ClipboardGateway {
        var currentText: String? = null
        var currentToken: String? = null
        var lastSensitive = false
        var clearCount = 0

        override fun putText(label: String, text: String, sensitive: Boolean, token: String) {
            currentText = text
            currentToken = token
            lastSensitive = sensitive
        }

        override fun clearIfOwned(token: String): Boolean {
            if (currentToken != token) return false
            currentText = null
            currentToken = null
            clearCount += 1
            return true
        }

        fun replaceExternally(text: String) {
            currentText = text
            currentToken = "external-token"
        }
    }

    private class FakeClipboardScheduler : ClipboardClearScheduler {
        var latest: FakeScheduledTask? = null

        override fun schedule(delayMillis: Long, action: () -> Unit): ScheduledClipboardClear {
            assertTrue(delayMillis > 0)
            return FakeScheduledTask(action).also { latest = it }
        }

        fun runLatest() {
            val task = latest ?: return
            if (!task.cancelled) task.action()
        }
    }

    private class FakeScheduledTask(
        val action: () -> Unit
    ) : ScheduledClipboardClear {
        var cancelled = false

        override fun cancel() {
            cancelled = true
        }

        fun runEvenIfCancelled() {
            action()
        }
    }
}
