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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.shary.app.ui.screens.home.utils.Screen
import com.shary.app.ui.screens.utils.PasswordOutlinedTextField
import com.shary.app.viewmodels.authentication.AuthEvent
import com.shary.app.viewmodels.authentication.AuthenticationMode
import com.shary.app.viewmodels.authentication.AuthenticationViewModel
import kotlinx.coroutines.launch

// Optional: Hilt entry point to reach CloudServiceImpl without polluting the function signature
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.shary.app.infrastructure.services.cloud.CloudServiceImpl
import dagger.hilt.EntryPoints

@EntryPoint
@InstallIn(SingletonComponent::class)
interface LogupScreenEntryPoints {
    fun cloudService(): CloudServiceImpl
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogupScreen(
    navController: NavHostController
) {
    val authenticationViewModel: AuthenticationViewModel = hiltViewModel()
    LaunchedEffect(Unit) { authenticationViewModel.setMode(AuthenticationMode.SIGNUP) }

    val ctx = LocalContext.current
    val form by authenticationViewModel.form.collectAsState()
    val loading by authenticationViewModel.loading.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }

    // Listen to one-shot events from the VM
    LaunchedEffect(Unit) {
        authenticationViewModel.events.collect { ev ->
            when (ev) {
                is AuthEvent.Success -> {
                    // OPTIONAL: register user in cloud after successful sign-up
                    val deps = EntryPoints.get(ctx.applicationContext, LogupScreenEntryPoints::class.java)
                    val cloud = deps.cloudService()
                    scope.launch {
                        val authToken = cloud.uploadUser(form.email)
                        if (authToken.isEmpty()) {
                            Toast.makeText(
                                ctx,
                                "The user couldn't be uploaded to the cloud",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(ctx, "User created successfully!", Toast.LENGTH_LONG).show()
                        }
                        // Navigate to Login (or Home if you prefer)
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Logup.route) { inclusive = true }
                        }
                    }
                }
                is AuthEvent.Error -> {
                    snackbarHostState.showSnackbar(ev.message)
                }
            }
        }
    }

    // Clear fields when screen goes to background (keeps your previous behavior)
    val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)
    DisposableEffect(lifecycleOwner.value) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) authenticationViewModel.resetForm()
        }
        val lifecycle = lifecycleOwner.value.lifecycle
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Logup") })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                value = form.email,
                onValueChange = authenticationViewModel::setEmail,
                label = { Text("Email") },
                singleLine = true,
                enabled = !loading,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = form.username,
                onValueChange = authenticationViewModel::setUsername,
                label = { Text("Username") },
                singleLine = true,
                enabled = !loading,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            PasswordOutlinedTextField(
                secret = form.password,
                isVisible = authenticationViewModel.passwordVisible,
                onValueChange = authenticationViewModel::setPassword,
                onClick = authenticationViewModel::togglePasswordVisibility
            )

            PasswordOutlinedTextField(
                secret = form.confirm,
                isVisible = authenticationViewModel.confirmVisible,
                onValueChange = authenticationViewModel::setConfirm,
                onClick = authenticationViewModel::toggleConfirmVisibility
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { authenticationViewModel.submit(ctx) },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (loading) "Creating..." else "Create Account")
            }
        }
    }
}
