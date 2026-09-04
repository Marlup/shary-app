package com.shary.app.application.email

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

class EmailShareControllerTest {
    @Test
    fun `only one concurrent preparation can start`() {
        val controller = EmailShareController()
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(8)

        try {
            val attempts = (1..32).map {
                executor.submit<Boolean> {
                    start.await()
                    controller.tryBeginPreparation(EmailShareKind.REQUEST)
                }
            }
            start.countDown()

            assertEquals(1, attempts.count { it.get() })
            assertEquals(
                EmailShareState.Preparing(EmailShareKind.REQUEST),
                controller.state.value
            )
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `prepared share is claimed once before chooser launch`() {
        val controller = EmailShareController()
        val lease = EmailShareLease(
            attachmentId = "attachment-id",
            expiresAtEpochMillis = 42L,
            kind = EmailShareKind.RESPONSE
        )

        assertTrue(controller.tryBeginPreparation(EmailShareKind.RESPONSE))
        assertTrue(controller.prepared(lease))
        assertFalse(controller.claimPrepared("another-id"))
        assertTrue(controller.claimPrepared(lease.attachmentId))
        assertFalse(controller.claimPrepared(lease.attachmentId))
        assertEquals(EmailShareState.Launching(lease), controller.state.value)
    }

    @Test
    fun `chooser handoff is terminal only as share sheet opened`() {
        val controller = EmailShareController()
        val lease = EmailShareLease(
            attachmentId = "attachment-id",
            expiresAtEpochMillis = 42L,
            kind = EmailShareKind.REQUEST
        )

        controller.tryBeginPreparation(EmailShareKind.REQUEST)
        controller.prepared(lease)
        controller.claimPrepared(lease.attachmentId)

        assertTrue(controller.shareSheetOpened(lease.attachmentId))
        assertEquals(EmailShareState.ShareSheetOpened(lease), controller.state.value)
        assertTrue(controller.tryBeginPreparation(EmailShareKind.REQUEST))
    }

    @Test
    fun `launch failure applies only to matching prepared attachment`() {
        val controller = EmailShareController()
        val lease = EmailShareLease(
            attachmentId = "attachment-id",
            expiresAtEpochMillis = 42L,
            kind = EmailShareKind.REQUEST
        )

        controller.tryBeginPreparation(EmailShareKind.REQUEST)
        controller.prepared(lease)

        assertFalse(controller.launchFailed("stale-id", "failure"))
        assertTrue(controller.launchFailed(lease.attachmentId, "failure"))
        assertEquals(
            EmailShareState.LaunchFailed(lease.attachmentId, "failure"),
            controller.state.value
        )
    }

    @Test
    fun `preparation failure releases the rapid tap gate`() {
        val controller = EmailShareController()

        assertTrue(controller.tryBeginPreparation(EmailShareKind.REQUEST))
        assertTrue(controller.preparationFailed(EmailShareKind.REQUEST, "failure"))
        assertTrue(controller.tryBeginPreparation(EmailShareKind.RESPONSE))
    }
}
