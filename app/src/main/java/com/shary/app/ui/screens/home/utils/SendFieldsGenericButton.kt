package com.shary.app.ui.screens.home.utils

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.ui.screens.utils.LongPressHint

@Composable
fun SendFieldsGenericButton(
    //fields: List<FieldDomain>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LongPressHint("Share the current data using another app") {
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
}
