package com.shary.app.ui.screens.summary

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.shary.app.core.domain.interfaces.navigator.HomeDepsEntryPoint
import com.shary.app.core.session.Session
import com.shary.app.ui.screens.home.utils.SendOption
import com.shary.app.ui.screens.home.utils.SendServiceDialog
import com.shary.app.ui.screens.home.utils.ShareFieldsGenericButton
import com.shary.app.ui.screens.summary.utils.SummaryTopAppBar
import com.shary.app.ui.screens.utils.GoBackButton
import com.shary.app.viewmodels.EmailViewModel
import dagger.hilt.EntryPoints

@Composable
fun SummaryScreen(navController: NavHostController) {
    val context = LocalContext.current
    val session: Session = remember {
        val ep = EntryPoints.get(context.applicationContext, HomeDepsEntryPoint::class.java)
        ep.session()
    }

    val emailViewModel: EmailViewModel = hiltViewModel()

    var sendOption by remember { mutableStateOf<SendOption?>(null) }
    var showSendDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        emailViewModel.intent.collect { intent ->
            context.startActivity(
                Intent.createChooser(intent, "Choose Email App")
            )
        }
    }

    Scaffold(
        topBar = { SummaryTopAppBar(navController) },
        modifier = Modifier.background(color = Color(0xFFF7F3FF)),
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                GoBackButton(navController)
                ShareFieldsGenericButton(
                    session.getCachedFields(),
                    onClick = { showSendDialog = true }
                )
            }
        }
    ) { padding ->

        if (showSendDialog) {
            SendServiceDialog(
                options = SendOption.all,
                onOptionSelected = { sendOption = it },
                onSendConfirmed = {
                    showSendDialog = false
                    when (sendOption) {

                        SendOption.Email -> {
                            emailViewModel.send(
                                session.getCachedFields(),
                                session.getCachedEmails()
                            )
                        }

                        SendOption.Cloud -> {
                            TODO()
                        }

                        null -> TODO()
                    }
                },
                onCancel = { showSendDialog = false }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "Summary before sending",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // ==== Table Header ====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Emails",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Start
                )
                Text(
                    text = "Fields",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Start
                )
            }

            HorizontalDivider()

            // ==== Table Rows ====
            val emails = session.getCachedEmails()
            val fields = session.getCachedFields()
            val maxRows = maxOf(emails.size, fields.size)

            LazyColumn {
                items(maxRows) { index ->
                    val email = emails.getOrNull(index) ?: ""
                    val field = fields.getOrNull(index)?.key ?: ""

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = email,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = field,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    HorizontalDivider()
                }
            }
        }
    }
}
