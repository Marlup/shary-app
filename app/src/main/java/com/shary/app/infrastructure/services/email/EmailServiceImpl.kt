package com.shary.app.infrastructure.services.email

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.shary.app.core.domain.interfaces.services.EmailService
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.session.Session
import com.shary.app.infrastructure.services.email.Constants.FILE_FORMATS
import com.shary.app.infrastructure.services.email.Constants.MSG_DEFAULT_SEND_FILENAME
import com.shary.app.utils.Functions.buildFileFromFields
import com.shary.app.utils.Functions.makeStringListFromFields
import jakarta.inject.Inject
import okio.ByteString.Companion.encodeUtf8
import java.io.File

class EmailServiceImpl @Inject constructor(
    private val context: Context,
    private val session: Session
): EmailService {

    override fun sendEmail(
        records: List<FieldDomain>,
        recipients: List<String>,
        filenameParam: String?,
        fileFormat: String
    ): Boolean
    {
        if (records.isEmpty() || recipients.isEmpty() || !FILE_FORMATS.contains(fileFormat)) {
            return false
        }
        val subject = "Shary message with ${records.size} fields"
        val filename = (filenameParam ?: "$MSG_DEFAULT_SEND_FILENAME${session.getOwnerEmail()}") + ".$fileFormat"

        val fileData = buildFileFromFields(records, fileFormat)
        if (fileData != null) {
            try {
                // Create temp file in cache dir
                val attachmentFile = File.createTempFile(filename, "", context.cacheDir).apply {
                    writeBytes(fileData.encodeUtf8().toByteArray())
                }

                // Get URI via FileProvider
                val uri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",  // Ensure this authority matches your manifest
                    attachmentFile
                )

                // Compose body text and build email intent
                val bodyText = buildBody(records, filename)

                val emailIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/octet-stream"
                    putExtra(Intent.EXTRA_EMAIL, recipients.toTypedArray())
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                    putExtra(Intent.EXTRA_TEXT, bodyText)
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                // Start chooser
                context.startActivity(Intent.createChooser(emailIntent, "Choose Email App"))

                return true
            } catch (e: Exception) {
                Log.e("EmailSend", "Failed to send email", e)
                Toast.makeText(context, "Error sending email: ${e.message}", Toast.LENGTH_LONG).show()
                return false
            }
        } else {
            Toast.makeText(context, "File data is empty", Toast.LENGTH_SHORT).show()
            return false
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

Best regards,
Shary Team""".trimIndent()
    }
}
