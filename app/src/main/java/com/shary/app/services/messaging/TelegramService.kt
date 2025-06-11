package com.shary.app.services.messaging

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.shary.app.Field
import com.shary.app.utils.UtilsFunctions.makeStringListFromFields

class TelegramService(private val context: Context) {

    fun sendFieldsToTelegram(fields: List<Field>, username: String? = null) {
        val message = buildMessage(fields)

        val uri = if (username != null) {
            // This opens chat with specific user (they must have allowed messages from anyone)
            Uri.parse("https://t.me/$username?text=${Uri.encode(message)}")
        } else {
            // This just opens Telegram with the message, no specific user
            Uri.parse("https://t.me/share/url?url=${Uri.encode(message)}")
        }

        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("org.telegram.messenger")
        }

        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            // Telegram not installed
        }
    }

    private fun buildMessage(fields: List<Field>): String {
        return makeStringListFromFields(fields)
    }
}
