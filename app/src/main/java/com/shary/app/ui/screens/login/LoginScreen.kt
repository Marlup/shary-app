// --- LoginScreen.kt ---
package com.shary.app.ui.screens.login

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.shary.app.ui.screens.home.utils.Screen
import com.shary.app.viewmodels.authentication.AuthEvent
import com.shary.app.viewmodels.authentication.AuthenticationMode
import com.shary.app.viewmodels.authentication.AuthenticationViewModel
import kotlinx.coroutines.launch

// Optional: small Hilt entry point if you still want to check registration on the cloud
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.shary.app.infrastructure.services.cloud.CloudServiceImpl
import dagger.hilt.EntryPoints

@EntryPoint
@InstallIn(SingletonComponent::class)
interface LoginScreenEntryPoints {
    fun cloudService(): CloudServiceImpl
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavHostController) {

    val authenticationViewModel: AuthenticationViewModel = hiltViewModel()
    LaunchedEffect(Unit) { authenticationViewModel.setMode(AuthenticationMode.LOGIN) }

    val context = LocalContext.current
    val activity = context as FragmentActivity
    val scope = rememberCoroutineScope()

    // VM state
    val form by authenticationViewModel.form.collectAsState()
    val loading by authenticationViewModel.loading.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Collect one-shot auth events
    LaunchedEffect(Unit) {
        authenticationViewModel.events.collect { ev ->
            when (ev) {
                is AuthEvent.Success -> {
                    // OPTIONAL: after successful login, check cloud registration
                    val deps = EntryPoints.get(context.applicationContext, LoginScreenEntryPoints::class.java)
                    val cloudService = deps.cloudService()
                    scope.launch {
                        val email = authenticationViewModel.authState.value.email
                        val registered = runCatching { cloudService.isUserRegistered(email) }.getOrDefault(false)
                        val msg = if (registered) "User is registered in Cloud" else "User is NOT registered in Cloud"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                    // Navigate to Home
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
                is AuthEvent.Error -> {
                    snackbarHostState.showSnackbar(ev.message)
                }
            }
        }
    }

    // Clear text fields when screen goes to background, like you had
    fun clearStates() {
        authenticationViewModel.setUsername("")
        authenticationViewModel.setPassword("")
    }
    val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)
    DisposableEffect(lifecycleOwner.value) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) clearStates()
        }
        val lifecycle = lifecycleOwner.value.lifecycle
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            Column(Modifier.fillMaxWidth()) {
                CenterAlignedTopAppBar(
                    title = { Text("Shary", style = MaterialTheme.typography.headlineMedium) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    ),
                    expandedHeight = 64.dp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                CenterAlignedTopAppBar(title = { Text("Login") })
            }
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
                value = form.username,
                onValueChange = authenticationViewModel::setUsername,
                label = { Text("Username") },
                singleLine = true,
                enabled = !loading,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = form.password,
                onValueChange = authenticationViewModel::setPassword,
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                enabled = !loading,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { authenticationViewModel.submit(context) },
                enabled = !loading,
                modifier = Modifier.size(200.dp, 50.dp)
            ) {
                Text(if (loading) "Checking..." else "Login")
            }

            /*
            Spacer(modifier = Modifier.height(32.dp))

            // Biometric Login (kept like your original; you can wire it to auth if desired)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = {
                        val biometricManager = BiometricAuthManager(
                            context = context,
                            activity = activity,
                            onAuthSuccess = {
                                // On biometric success, go Home.
                                // If you want biometric to actually sign in, call authenticationViewModel.submit(context) or provide a biometric path.
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }
                            }
                        )
                        val error = biometricManager.authenticate()
                        if (error != null) errorMessage = error
                    },
                    enabled = !loading,
                    modifier = Modifier.size(200.dp, 50.dp),
                ) { Text("Biometric Login") }

                errorMessage?.let {
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(text = it, color = MaterialTheme.colorScheme.error)
                }
            }

             */
        }
    }
}
