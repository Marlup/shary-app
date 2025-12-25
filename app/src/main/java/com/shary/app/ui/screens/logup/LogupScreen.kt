// --- LogupScreen.kt ---
package com.shary.app.ui.screens.logup

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
//import androidx.hilt.navigation.compose.hiltViewModel // deprecated location of hiltViewModel
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.shary.app.core.domain.interfaces.viewmodels.AuthenticationEvent
import com.shary.app.ui.screens.home.utils.Screen
import com.shary.app.ui.screens.utils.PasswordOutlinedTextField
import com.shary.app.viewmodels.authentication.AuthenticationMode
import com.shary.app.viewmodels.authentication.AuthenticationViewModel
import kotlinx.coroutines.launch

// Optional: Hilt entry point to reach CloudServiceImpl without polluting the function signature
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.shary.app.infrastructure.services.cloud.CloudServiceImpl
import com.shary.app.viewmodels.authentication.AuthLogForm
import com.shary.app.viewmodels.communication.CloudViewModel
import dagger.hilt.EntryPoints

@EntryPoint
@InstallIn(SingletonComponent::class)
interface LogupScreenEntryPoints {
    fun cloudService(): CloudServiceImpl
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogupScreen(navController: NavHostController) {

    // ---------------- ViewModels ----------------
    val authenticationViewModel: AuthenticationViewModel = hiltViewModel()
    val cloudViewModel: CloudViewModel = hiltViewModel()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ---------------- ViewModels States ----------------
    val signupForm by authenticationViewModel.logForm.collectAsState()
    val loading by authenticationViewModel.loading.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { authenticationViewModel.setMode(AuthenticationMode.SIGNUP) }

    // Listen to one-shot events from the VM
    LaunchedEffect(Unit) {
        authenticationViewModel.events.collect { ev ->
            when (ev) {
                is AuthenticationEvent.Success -> {
                    scope.launch {
                        authenticationViewModel.onLoginSuccess(signupForm.username)
                        val authToken = authenticationViewModel.getToken()
                        if (authToken.isNullOrEmpty()) {
                            Toast.makeText(
                                context,
                                "The user couldn't be uploaded to the cloud",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(context, "User created successfully!", Toast.LENGTH_LONG).show()
                        }
                        // Navigate to Login (or Home if you prefer)
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Logup.route) { inclusive = true }
                        }
                    }
                }

                is AuthenticationEvent.Error -> {
                    snackbarHostState.showSnackbar(ev.message)
                }

                is AuthenticationEvent.CloudAnonymousReady -> {
                    // Cloud anonymous session established
                    val msg = "Anonymous cloud session ready (uid=${ev.uid.take(8)}…)"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    Log.i("LogupEvents", msg)
                    // Optional: automatically upload user after anonymous session
                    scope.launch {
                        cloudViewModel.uploadUser(signupForm.username)
                        val authToken = authenticationViewModel.getToken()
                            if (authToken.isNullOrEmpty()) {
                            Log.i("LogupEvents", "User uploaded after anonymous connect")
                        }
                    }
                }

                AuthenticationEvent.CloudSignedOut -> {
                    // Cloud session signed out
                    val msg = "Cloud session signed out"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    Log.i("LogupEvents", msg)
                    // Navigate back to Login
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Logup.route) { inclusive = true }
                    }
                }

                is AuthenticationEvent.CloudTokenRefreshed -> {
                    // Cloud token refreshed
                    val msg = "Cloud token refreshed"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    Log.i("LogupEvents", "New token: ${ev.token.take(12)}…")
                    // Optional: you could retry a failed uploadUser here with fresh token
                }

                is AuthenticationEvent.UserRegisteredInCloud -> {
                    Toast.makeText(context, "User is registered in Cloud", Toast.LENGTH_SHORT).show()
                }

                is AuthenticationEvent.UserNotRegisteredInCloud -> {
                    Toast.makeText(context, "User is NOT registered in Cloud", Toast.LENGTH_SHORT).show()
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
            CenterAlignedTopAppBar(
                title = { Text("Logup") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                expandedHeight = 64.dp
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {  }
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
                value = signupForm.email,
                onValueChange = { authenticationViewModel.updateEmail(it) },
                label = { Text("Email") },
                singleLine = true,
                enabled = !loading,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = signupForm.username,
                onValueChange = { authenticationViewModel.updateUsername(it) },
                label = { Text("Username") },
                singleLine = true,
                enabled = !loading,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            PasswordOutlinedTextField(
                secret = signupForm.password,
                isVisible = authenticationViewModel.passwordVisible,
                onValueChange = { authenticationViewModel.updatePassword(it) },
                onClick = authenticationViewModel::togglePasswordVisibility
            )

            PasswordOutlinedTextField(
                secret = signupForm.passwordConfirm,
                isVisible = authenticationViewModel.confirmVisible,
                onValueChange = { authenticationViewModel.updatePasswordConfirm(it) },
                onClick = authenticationViewModel ::toggleConfirmVisibility
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { authenticationViewModel.submit(context) },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (loading) "Creating..." else "Create Account")
            }
        }
    }
}
