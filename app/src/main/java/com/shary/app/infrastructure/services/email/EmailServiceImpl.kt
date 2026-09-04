package com.shary.app.infrastructure.services.email

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.shary.app.core.domain.interfaces.services.EmailService
import com.shary.app.core.domain.interfaces.services.JsonFileService
import com.shary.app.core.domain.interfaces.services.PreparedEmailShare
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.models.UserDomain
import com.shary.app.core.domain.types.enums.DataFileMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

// infrastructure/services/email/EmailServiceImpl.kt
class EmailServiceImpl @Inject constructor(
    private val context: Context,
    private val jsonFileService: JsonFileService,
    private val attachmentStore: EmailAttachmentStore,
    private val cleanupCoordinator: EmailAttachmentCleanupCoordinator
) : EmailService {

    override suspend fun prepareRequestShare(
        fields: List<FieldDomain>,
        recipients: List<UserDomain>
    ) = prepareShare(
        fields = fields,
        recipients = recipients,
        subject = "Shary request",
        metadata = mapOf("mode" to "request"),
        mode = DataFileMode.Request
    )

    override suspend fun prepareResponseShare(
        fields: List<FieldDomain>,
        recipients: List<UserDomain>
    ) = prepareShare(
        fields = fields,
        recipients = recipients,
        subject = "Shary response",
        metadata = mapOf("mode" to "response"),
        mode = DataFileMode.Response
    )

    override suspend fun retainPreparedShare(attachmentId: String): Boolean =
        withContext(Dispatchers.IO) {
            attachmentStore.findLease(attachmentId)?.let(cleanupCoordinator::schedule) ?: false
        }

    override suspend fun discardPreparedShare(attachmentId: String): Boolean =
        withContext(Dispatchers.IO) {
            cleanupCoordinator.deleteNow(attachmentId)
        }

    private suspend fun prepareShare(
        fields: List<FieldDomain>,
        recipients: List<UserDomain>,
        subject: String,
        metadata: Map<String, String>,
        mode: DataFileMode
    ): Result<PreparedEmailShare> = withContext(Dispatchers.IO) {
        var source: File? = null
        var lease: EmailAttachmentLease? = null
        runCatching {
            require(fields.isNotEmpty()) { "At least one field is required." }
            require(recipients.isNotEmpty()) { "At least one recipient is required." }
            cleanupCoordinator.sweepExpiredAndReschedule()

            source = jsonFileService.createSharyJson(fields, metadata, mode)
            lease = attachmentStore.adopt(requireNotNull(source))
            source = null

            val adopted = requireNotNull(lease)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                adopted.file
            )
            val fieldSummary = fields.joinToString("\n") { "- ${it.key}" }
            val body = """
                Hello

                Attached file: $subject.
                Fields included:

                $fieldSummary
            """.trimIndent()

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                clipData = ClipData.newRawUri("Shary JSON attachment", uri)
                putExtra(Intent.EXTRA_EMAIL, recipients.map { it.email }.toTypedArray())
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            cleanupCoordinator.schedule(adopted)
            PreparedEmailShare(
                attachmentId = adopted.attachmentId,
                intent = intent,
                expiresAtEpochMillis = adopted.expiresAtEpochMillis
            )
        }.onFailure {
            lease?.let { cleanupCoordinator.deleteNow(it.attachmentId) }
            source?.let(::deleteJsonPreparationSource)
        }
    }

    private fun deleteJsonPreparationSource(file: File) {
        val cacheRoot = runCatching { context.cacheDir.canonicalFile }.getOrNull() ?: return
        val candidate = runCatching { file.canonicalFile }.getOrNull() ?: return
        val isOwnedPreparationFile = candidate.parentFile == cacheRoot &&
            candidate.name.startsWith("shary_") &&
            candidate.extension.equals("json", ignoreCase = true)
        if (isOwnedPreparationFile) candidate.delete()
    }
}
