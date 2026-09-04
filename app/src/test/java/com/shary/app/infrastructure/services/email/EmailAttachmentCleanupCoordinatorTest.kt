package com.shary.app.infrastructure.services.email

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class EmailAttachmentCleanupCoordinatorTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `scheduled attachment is deleted when its lease expires`() = runBlocking {
        val directory = temporaryFolder.newFolder("email-attachments")
        val store = EmailAttachmentStore(
            attachmentsDirectory = directory,
            leaseDurationMillis = 0L
        )
        val lease = store.adopt(temporaryFolder.newFile("source.json").apply { writeText("data") })
        val coordinator = EmailAttachmentCleanupCoordinator(store)

        try {
            coordinator.schedule(lease)
            withTimeout(2_000L) {
                while (lease.file.exists()) delay(10L)
            }
            assertFalse(lease.file.exists())
        } finally {
            coordinator.stopForTest()
        }
    }

    @Test
    fun `startup sweep removes attachments that expired while app was stopped`() = runBlocking {
        var now = 1_000L
        val directory = temporaryFolder.newFolder("email-attachments")
        val store = EmailAttachmentStore(
            attachmentsDirectory = directory,
            nowMillis = { now },
            leaseDurationMillis = 100L
        )
        val lease = store.adopt(temporaryFolder.newFile("source.json").apply { writeText("data") })
        now = 1_101L
        val coordinator = EmailAttachmentCleanupCoordinator(store, nowMillis = { now })

        try {
            coordinator.start()
            withTimeout(2_000L) {
                while (lease.file.exists()) delay(10L)
            }
            assertFalse(lease.file.exists())
        } finally {
            coordinator.stopForTest()
        }
    }
}
