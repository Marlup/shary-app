package com.shary.app.services.messaging

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.shary.app.Field
import com.shary.app.utils.DateUtils

class WhatsAppService(private val context: Context) {

    fun sendFieldsToWhatsApp(fields: List<Field>, phoneNumber: String? = null) {
        val message = buildMessage(fields)
        val uri = if (phoneNumber != null) {
            Uri.parse("https://wa.me/${phoneNumber}?text=${Uri.encode(message)}")
        } else {
            Uri.parse("https://wa.me/?text=${Uri.encode(message)}")
        }

        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.whatsapp")
        }

        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            // Handle error: WhatsApp not installed
        }
    }

    private fun buildMessage(fields: List<Field>): String {
        return fields.joinToString("\n") {
            val formattedDate = DateUtils.formatTimeMillis(it.dateAdded)
            "${it.key}: ${it.value} (added on $formattedDate)"
        }
    }
}
