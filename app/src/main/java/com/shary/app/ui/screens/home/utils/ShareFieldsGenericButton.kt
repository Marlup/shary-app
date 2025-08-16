package com.shary.app.ui.screens.home.utils

import android.content.Intent
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.shary.app.Field
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.utils.DateUtils

@Composable
fun ShareFieldsGenericButton(
    fields: List<FieldDomain>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Button(
        onClick = {
            onClick()
            /*
            val message = fields.joinToString("\n") {
                val formattedDate = DateUtils.formatTimeMillis(it.dateAdded)
                "${it.key}: ${it.value} (added on $formattedDate)"
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
                // You can optionally add a subject (for email clients)
                putExtra(Intent.EXTRA_SUBJECT, "Shared Data from Shary App")
            }

            val chooser = Intent.createChooser(shareIntent, "Share fields using...")
            context.startActivity(chooser)
            */

        },
        modifier = modifier
    ) {
        Text("Share")
    }
}
