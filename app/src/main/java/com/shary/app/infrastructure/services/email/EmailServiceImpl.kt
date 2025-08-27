package com.shary.app.infrastructure.services.email

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.shary.app.core.domain.interfaces.services.EmailService
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.session.Session
import com.shary.app.infrastructure.services.email.Constants.FILE_FORMATS
import com.shary.app.infrastructure.services.email.Constants.MSG_DEFAULT_SEND_FILENAME
import com.shary.app.utils.Functions.buildFileFromFields
import com.shary.app.utils.Functions.makeStringListFromFields
import javax.inject.Inject
import okio.ByteString.Companion.encodeUtf8
import java.io.File

class EmailServiceImpl @Inject constructor(
    private val context: Context,
    private val session: Session
) : EmailService {

    override fun buildEmail(
        records: List<FieldDomain>,
        recipients: List<String>,
        filenameParam: String?,
        fileFormat: String
    ): Result<Intent> = runCatching {
        require(records.isNotEmpty()) { "No records to send" }
        require(recipients.isNotEmpty()) { "No recipients provided" }
        require(FILE_FORMATS.contains(fileFormat)) { "Unsupported file format: $fileFormat" }

        val subject = "Shary message with ${records.size} fields"
        val filename = (filenameParam ?: "${MSG_DEFAULT_SEND_FILENAME}${session.getOwnerEmail()}") + ".$fileFormat"

        val fileData = buildFileFromFields(records, fileFormat)
            ?: error("File data is empty")

        val attachmentFile = File.createTempFile(filename, "", context.cacheDir).apply {
            writeBytes(fileData.encodeUtf8().toByteArray())
        }

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            attachmentFile
        )

        val bodyText = buildBody(records, filename)

        Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_EMAIL, recipients.toTypedArray())
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, bodyText)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun buildBody(records: List<FieldDomain>, filename: String): String {
        val sharyUri = "http://localhost:5001/files/open?filename=./$filename"
        val messageKeys = makeStringListFromFields(records)
        return """
Hello,

I made this email info. by Shary-App.

Fields:

$messageKeys

Download file: $sharyUri

Best regards,
Shary Team
""".trimIndent()
    }
}

