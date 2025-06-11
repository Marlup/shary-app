package com.shary.app.services.email

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.shary.app.Field
import com.shary.app.core.Session
import com.shary.app.services.email.Constants.FILE_FORMATS
import com.shary.app.services.email.Constants.MSG_DEFAULT_SEND_FILENAME
import com.shary.app.utils.UtilsFunctions.buildFileFromFields
import com.shary.app.utils.UtilsFunctions.makeStringListFromFields
import okio.ByteString.Companion.encodeUtf8
import java.io.File

class EmailService(
    private val context: Context,
    private val session: Session
) {

    fun sendEmailViaClient(
        records: List<Field>,
        recipients: List<String>,
        filenameParam: String? = "SharyFile",
        fileFormat: String = "json"
    ): Boolean
    {
        if (records.isEmpty() || recipients.isEmpty() || !FILE_FORMATS.contains(fileFormat)) {
            return false
        }
        val subject = "Shary message with ${records.size} fields"
        val filename = (filenameParam ?: "$MSG_DEFAULT_SEND_FILENAME${session.username}") + ".$fileFormat"

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
                val bodyText = buildEmailBody(records, filename)

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

    private fun buildEmailBody(records: List<Field>, filename: String): String {
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
