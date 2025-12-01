package com.shary.app.infrastructure.services.email

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.shary.app.core.domain.interfaces.services.EmailService
import com.shary.app.core.domain.interfaces.services.JsonFileService
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.models.UserDomain
import com.shary.app.core.domain.types.enums.DataFileMode
import javax.inject.Inject

// infrastructure/services/email/EmailServiceImpl.kt
class EmailServiceImpl @Inject constructor(
    private val context: Context,
    private val jsonFileService: JsonFileService
) : EmailService {

    override suspend fun sendRequestFile(fields: List<FieldDomain>, recipients: List<UserDomain>) =
        buildAndSend(fields, recipients, "Shary request", mapOf("mode" to "request"), DataFileMode.Request)

    override suspend fun sendResponseFile(fields: List<FieldDomain>, recipients: List<UserDomain>) =
        buildAndSend(fields, recipients, "Shary response", mapOf("mode" to "response"), DataFileMode.Response)

    private suspend fun buildAndSend(
        fields: List<FieldDomain>,
        recipients: List<UserDomain>,
        subject: String,
        metadata: Map<String, String>,
        mode: DataFileMode
    ): Result<Intent> = runCatching {
        val json = jsonFileService.createSharyJson(fields, metadata, mode)
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", json
        )

        val fieldSummary = fields.joinToString("\n") { "- ${it.key}" }
        val body = """
            Hello
            
            Attached file $subject.
            Fields included:
            
            $fieldSummary
        """.trimIndent()

        Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_EMAIL, recipients.map { it.email }.toTypedArray())
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
