package com.shary.app.infrastructure.services.email

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.UUID

class EmailAttachmentStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `adopt moves source into dedicated directory with bounded lease`() {
        var now = 10_000L
        val directory = temporaryFolder.newFolder("cache", EmailAttachmentStore.DIRECTORY_NAME)
        val source = temporaryFolder.newFile("shary_source.json").apply {
            writeText("{\"fields\":{\"secret\":\"readable\"}}")
        }
        val store = EmailAttachmentStore(directory, nowMillis = { now })

        val lease = store.adopt(source)

        assertFalse(source.exists())
        assertEquals(directory.canonicalFile, lease.file.parentFile.canonicalFile)
        assertEquals("{\"fields\":{\"secret\":\"readable\"}}", lease.file.readText())
        assertEquals(now, lease.createdAtEpochMillis)
        assertEquals(now + EmailAttachmentStore.ATTACHMENT_LEASE_MILLIS, lease.expiresAtEpochMillis)
        assertEquals(lease, store.findLease(lease.attachmentId))
    }

    @Test
    fun `delete is contained and idempotent`() {
        val directory = temporaryFolder.newFolder("email-attachments")
        val outside = temporaryFolder.newFile("outside.json").apply { writeText("keep") }
        var revokeCount = 0
        val store = EmailAttachmentStore(
            attachmentsDirectory = directory,
            revokeReadGrant = { revokeCount += 1 }
        )
        val lease = store.adopt(temporaryFolder.newFile("source.json").apply { writeText("data") })

        assertFalse(store.delete("../${outside.name}"))
        assertTrue(outside.exists())
        assertTrue(store.delete(lease.attachmentId))
        assertTrue(store.delete(lease.attachmentId))
        assertEquals(1, revokeCount)
        assertTrue(outside.exists())
    }

    @Test
    fun `expired sweep deletes only owned stale artifacts`() {
        var now = 1_000L
        val directory = temporaryFolder.newFolder("email-attachments")
        val store = EmailAttachmentStore(
            attachmentsDirectory = directory,
            nowMillis = { now },
            leaseDurationMillis = 100L
        )
        val expired = store.adopt(sourceFile("expired-source.json"))

        now = 1_050L
        val fresh = store.adopt(sourceFile("fresh-source.json"))
        val unrelated = File(directory, "unrelated.json").apply { writeText("keep") }
        val ownedPartial = File(directory, "${UUID.randomUUID()}.part").apply {
            writeText("partial")
            setLastModified(1_000L)
        }

        now = 1_101L
        assertEquals(2, store.sweepExpired())
        assertFalse(expired.file.exists())
        assertFalse(ownedPartial.exists())
        assertTrue(fresh.file.exists())
        assertTrue(unrelated.exists())
    }

    @Test
    fun `fresh leases survive restart sweep and remain discoverable`() {
        var now = 5_000L
        val directory = temporaryFolder.newFolder("email-attachments")
        val originalStore = EmailAttachmentStore(directory, nowMillis = { now }, leaseDurationMillis = 1_000L)
        val lease = originalStore.adopt(sourceFile("source.json"))

        now = 5_999L
        val restartedStore = EmailAttachmentStore(directory, nowMillis = { now }, leaseDurationMillis = 1_000L)

        assertEquals(0, restartedStore.sweepExpired())
        assertEquals(listOf(lease.attachmentId), restartedStore.listActiveLeases().map { it.attachmentId })
    }

    private fun sourceFile(name: String): File =
        temporaryFolder.newFile(name).apply { writeText("{\"fields\":{}}") }
}
