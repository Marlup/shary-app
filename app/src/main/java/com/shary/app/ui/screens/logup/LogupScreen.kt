// --- LogupScreen.kt ---

package com.shary.app.ui.screens.logup

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.shary.app.core.Session
import com.shary.app.services.cloud.CloudService
import com.shary.app.ui.screens.home.Screen
import com.shary.app.ui.screens.ui_utils.PasswordOutlinedTextField
import com.shary.app.utils.ValidationUtils.validateLogupCredentials
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogupScreen(
    navController: NavHostController,
    session: Session,
    cloudService: CloudService
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Logup") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            PasswordOutlinedTextField(
                password,
                passwordVisible,
                onValueChange = { password = it },
                onClick = { passwordVisible = !passwordVisible }
            )

            Spacer(modifier = Modifier.height(16.dp))

            PasswordOutlinedTextField(
                confirmPassword,
                confirmPasswordVisible,
                onValueChange = { confirmPassword = it },
                onClick = { confirmPasswordVisible = !confirmPasswordVisible }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val message = validateLogupCredentials(email, username, password, confirmPassword)
                    if (message.isBlank()) {
                        // Cache Credentials from UI
                        session.cacheCredentials(email, username, password)

                        // Generate keys on the fly
                        session.generateKeys(password, username)

                        scope.launch {
                            // Upload user to the cloud
                            val (success, token) = cloudService.uploadUser(email)
                            if (success) {
                                // Set verification token
                                session.setVerificationToken(token)
                            } else {
                                Toast.makeText(context,
                                    "The user couldn't be uploaded to the cloud",
                                    Toast.LENGTH_LONG).show()
                            }
                        }
                        // Store cached credentials
                        session.storeCachedCredentials(context)

                        // Go to Login Screen
                        navController.navigate(Screen.Login.route)
                        Toast.makeText(context, "User created successfully!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create Account")
            }
        }
    }
}
