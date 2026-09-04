package com.shary.app.infrastructure.services.email

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID

private const val EMAIL_ATTACHMENT_DIRECTORY = "email-attachments"
private const val DEFAULT_EMAIL_ATTACHMENT_LEASE_MILLIS = 24L * 60L * 60L * 1_000L

private fun readGrantRevoker(context: Context): (File) -> Unit = { file ->
    runCatching {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }.getOrNull()?.let { uri ->
        context.revokeUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}

data class EmailAttachmentLease(
    val attachmentId: String,
    val file: File,
    val createdAtEpochMillis: Long,
    val expiresAtEpochMillis: Long
)

/**
 * Owns temporary plaintext email attachments beneath one dedicated cache
 * directory. IDs are validated before lookup and no cleanup operation can
 * resolve outside that directory.
 */
class EmailAttachmentStore(
    private val attachmentsDirectory: File,
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val leaseDurationMillis: Long = DEFAULT_EMAIL_ATTACHMENT_LEASE_MILLIS,
    private val revokeReadGrant: (File) -> Unit = {}
) {
    init {
        require(leaseDurationMillis >= 0L) { "Email attachment lease cannot be negative." }
    }

    constructor(context: Context) : this(
        attachmentsDirectory = File(context.cacheDir, EMAIL_ATTACHMENT_DIRECTORY),
        revokeReadGrant = readGrantRevoker(context)
    )

    @Synchronized
    @Throws(IOException::class)
    fun adopt(source: File): EmailAttachmentLease {
        require(source.isFile) { "Email attachment source does not exist." }
        ensureDirectory()

        val attachmentId = UUID.randomUUID().toString()
        val target = ownedAttachmentFile(attachmentId)
            ?: throw IOException("Could not resolve email attachment target.")
        val partial = File(attachmentsDirectory, "$attachmentId$PARTIAL_SUFFIX")

        try {
            moveOrCopy(source, target, partial)
            val requestedCreatedAt = nowMillis()
            val createdAt = if (target.setLastModified(requestedCreatedAt)) {
                requestedCreatedAt
            } else {
                target.lastModified()
            }
            return leaseFor(target, createdAt)
        } catch (error: Exception) {
            partial.delete()
            target.delete()
            throw IOException("Could not adopt email attachment.", error)
        }
    }

    @Synchronized
    fun findLease(attachmentId: String): EmailAttachmentLease? {
        val file = ownedAttachmentFile(attachmentId) ?: return null
        if (!file.isFile) return null
        return leaseFor(file, file.lastModified())
    }

    @Synchronized
    fun listActiveLeases(): List<EmailAttachmentLease> {
        ensureDirectory()
        val now = nowMillis()
        return attachmentsDirectory.listFiles()
            .orEmpty()
            .filter { it.isFile && ATTACHMENT_FILE.matches(it.name) }
            .map { leaseFor(it, it.lastModified()) }
            .filter { it.expiresAtEpochMillis > now }
    }

    /** Returns true when the target is already absent, making retries safe. */
    @Synchronized
    fun delete(attachmentId: String): Boolean {
        val file = ownedAttachmentFile(attachmentId) ?: return false
        if (!file.exists()) return true
        runCatching { revokeReadGrant(file) }
        return file.delete() || !file.exists()
    }

    @Synchronized
    fun sweepExpired(): Int {
        ensureDirectory()
        val now = nowMillis()
        var deleted = 0

        attachmentsDirectory.listFiles().orEmpty().forEach { file ->
            if (!file.isFile || !isOwnedArtifactName(file.name)) return@forEach
            val expiresAt = safeExpiry(file.lastModified())
            if (expiresAt > now) return@forEach

            if (ATTACHMENT_FILE.matches(file.name)) {
                val attachmentId = file.name.removeSuffix(ATTACHMENT_SUFFIX)
                if (delete(attachmentId)) deleted += 1
            } else if (file.delete() || !file.exists()) {
                deleted += 1
            }
        }
        return deleted
    }

    private fun ensureDirectory() {
        if (!attachmentsDirectory.exists() && !attachmentsDirectory.mkdirs()) {
            throw IOException("Could not create email attachment directory.")
        }
        if (!attachmentsDirectory.isDirectory) {
            throw IOException("Email attachment path is not a directory.")
        }
    }

    private fun ownedAttachmentFile(attachmentId: String): File? {
        val canonicalId = runCatching { UUID.fromString(attachmentId).toString() }.getOrNull()
            ?: return null
        if (canonicalId != attachmentId) return null

        val root = runCatching { attachmentsDirectory.canonicalFile }.getOrNull() ?: return null
        val candidate = runCatching {
            File(root, "$canonicalId$ATTACHMENT_SUFFIX").canonicalFile
        }.getOrNull() ?: return null
        return candidate.takeIf { it.parentFile == root }
    }

    private fun moveOrCopy(source: File, target: File, partial: File) {
        val atomicMove = runCatching {
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE)
        }
        if (atomicMove.isSuccess) return

        FileOutputStream(partial).use { output ->
            source.inputStream().use { input -> input.copyTo(output) }
            output.fd.sync()
        }
        runCatching {
            Files.move(partial.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE)
        }.getOrElse {
            Files.move(partial.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
        if (!source.delete() && source.exists()) {
            throw IOException("Could not remove adopted email attachment source.")
        }
    }

    private fun leaseFor(file: File, createdAt: Long): EmailAttachmentLease {
        val attachmentId = file.name.removeSuffix(ATTACHMENT_SUFFIX)
        return EmailAttachmentLease(
            attachmentId = attachmentId,
            file = file,
            createdAtEpochMillis = createdAt,
            expiresAtEpochMillis = safeExpiry(createdAt)
        )
    }

    private fun safeExpiry(createdAt: Long): Long {
        return if (createdAt > Long.MAX_VALUE - leaseDurationMillis) {
            Long.MAX_VALUE
        } else {
            createdAt + leaseDurationMillis
        }
    }

    private fun isOwnedArtifactName(name: String): Boolean {
        return ATTACHMENT_FILE.matches(name) || PARTIAL_FILE.matches(name)
    }

    companion object {
        const val DIRECTORY_NAME = EMAIL_ATTACHMENT_DIRECTORY
        const val ATTACHMENT_LEASE_MILLIS = DEFAULT_EMAIL_ATTACHMENT_LEASE_MILLIS

        private const val ATTACHMENT_SUFFIX = ".json"
        private const val PARTIAL_SUFFIX = ".part"
        private val UUID_PATTERN =
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
        private val ATTACHMENT_FILE = Regex("^$UUID_PATTERN\\.json$")
        private val PARTIAL_FILE = Regex("^$UUID_PATTERN\\.part$")
    }
}
