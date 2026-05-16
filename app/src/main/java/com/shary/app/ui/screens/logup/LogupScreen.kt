package com.shary.app.ui.screens.logup

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavHostController
import com.shary.app.core.domain.interfaces.navigator.CredentialsEntryPoint
import com.shary.app.core.domain.interfaces.persistance.CredentialsStore
import com.shary.app.core.domain.interfaces.security.CryptographyManager
import com.shary.app.core.domain.interfaces.viewmodels.AuthenticationEvent
import com.shary.app.ui.components.SharyPrimaryButton
import com.shary.app.ui.components.SharySoftButton
import com.shary.app.ui.components.SharyTextButton
import com.shary.app.ui.screens.home.utils.Screen
import com.shary.app.ui.screens.utils.LoadingOverlay
import com.shary.app.ui.screens.utils.PasswordOutlinedTextField
import com.shary.app.ui.theme.SharyRadius
import com.shary.app.ui.theme.SurfaceLight
import com.shary.app.ui.theme.Violet600
import com.shary.app.utils.log.AppLogger
import com.shary.app.viewmodels.authentication.AuthenticationMode
import com.shary.app.viewmodels.authentication.AuthenticationViewModel
import com.shary.app.viewmodels.communication.CloudViewModel
import com.shary.app.viewmodels.configuration.SettingsViewModel
import dagger.hilt.EntryPoints
import kotlinx.coroutines.launch

@Composable
fun LogupScreen(navController: NavHostController) {
    val authenticationViewModel: AuthenticationViewModel = hiltViewModel()
    val cloudViewModel: CloudViewModel = hiltViewModel()
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    val signupForm by authenticationViewModel.logForm.collectAsState()
    val loading by authenticationViewModel.loading.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val creds: CredentialsStore = remember {
        val ep = EntryPoints.get(context.applicationContext, CredentialsEntryPoint::class.java)
        ep.credentialsStore()
    }
    val crypto: CryptographyManager = remember {
        val ep = EntryPoints.get(context.applicationContext, CredentialsEntryPoint::class.java)
        ep.cryptographyManager()
    }
    var locked by remember { mutableStateOf(creds.isCredentialsLocked(context)) }

    LaunchedEffect(Unit) { authenticationViewModel.setMode(AuthenticationMode.SIGNUP) }

    LaunchedEffect(Unit) {
        authenticationViewModel.events.collect { ev ->
            when (ev) {
                is AuthenticationEvent.Success -> {
                    scope.launch {
                        creds.clearCredentialsLocked(context)
                        locked = false
                        authenticationViewModel.onLoginSuccess(signupForm.email)
                        val authToken = authenticationViewModel.getToken()
                        if (authToken.isNullOrEmpty()) {
                            Toast.makeText(
                                context,
                                "Account created. Check your inbox to verify email before cloud sync.",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(context, "User created successfully!", Toast.LENGTH_LONG).show()
                        }
                        settingsViewModel.rememberEmailIfEnabled(signupForm.email)
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Signup.route) { inclusive = true }
                        }
                    }
                }
                is AuthenticationEvent.Error -> snackbarHostState.showSnackbar(ev.message)
                is AuthenticationEvent.CloudAnonymousReady -> {
                    val msg = "Anonymous cloud session ready (uid=${ev.uid.take(8)}…)"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    AppLogger.info("SignupEvents", "event=cloud_anonymous_ready")
                    scope.launch { cloudViewModel.uploadUser(signupForm.email) }
                }
                is AuthenticationEvent.CloudSignedOut -> {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Signup.route) { inclusive = true }
                    }
                }
                else -> Unit
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) authenticationViewModel.resetForm()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    ApplySignupStatusBar()

    LoadingOverlay(isLoading = loading) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SurfaceLight)
        ) {
            SignupHero()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (locked) {
                    Text(
                        text = "Existing credentials are locked on this device. You can retry unlock or continue with a new account.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(10.dp))
                    SharySoftButton(
                        text = "Retry unlock",
                        onClick = {
                            val usable = creds.hasUsableCredentials(context, crypto)
                            locked = !usable
                            if (usable) {
                                Toast.makeText(context, "Credentials unlocked. Please sign in.", Toast.LENGTH_LONG).show()
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(Screen.Signup.route) { inclusive = true }
                                }
                            } else {
                                Toast.makeText(context, "Still locked. Try again later.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !loading,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                }
                OutlinedTextField(
                    value = signupForm.email,
                    onValueChange = authenticationViewModel::updateEmail,
                    label = { Text("Email") },
                    singleLine = true,
                    enabled = !loading,
                    shape = SharyRadius.input,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = signupForm.username,
                    onValueChange = authenticationViewModel::updateUsername,
                    label = { Text("Username") },
                    singleLine = true,
                    enabled = !loading,
                    shape = SharyRadius.input,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(14.dp))

                PasswordOutlinedTextField(
                    secret = signupForm.password,
                    isVisible = authenticationViewModel.passwordVisible,
                    onValueChange = authenticationViewModel::updatePassword,
                    onClick = authenticationViewModel::togglePasswordVisibility,
                    label = "Password"
                )
                Spacer(Modifier.height(10.dp))
                PasswordOutlinedTextField(
                    secret = signupForm.passwordConfirm,
                    isVisible = authenticationViewModel.confirmVisible,
                    onValueChange = authenticationViewModel::updatePasswordConfirm,
                    onClick = authenticationViewModel::toggleConfirmVisibility,
                    label = "Confirm Password"
                )

                Spacer(Modifier.height(20.dp))
                SharyPrimaryButton(
                    text = if (loading) "Creating..." else "Create Account",
                    onClick = { authenticationViewModel.submit(context) },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth()
                )
                SharyTextButton(
                    text = "Already have an account? Sign in",
                    onClick = { navController.navigate(Screen.Login.route) },
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        SnackbarHost(hostState = snackbarHostState)
    }
}

@Composable
private fun SignupHero() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Violet600)
            .padding(horizontal = 24.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .border(1.dp, Color.White.copy(alpha = 0.2f), SharyRadius.avatar)
                .background(Color.White.copy(alpha = 0.2f), SharyRadius.avatar)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "S",
                style = MaterialTheme.typography.displayMedium,
                color = Color.White
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Shary",
            style = MaterialTheme.typography.displayMedium,
            color = Color.White
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Create your secure workspace.\nShare only what you choose.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.65f)
        )
    }
}

@Composable
private fun ApplySignupStatusBar() {
    val context = LocalContext.current
    val activity = context as? Activity ?: return
    DisposableEffect(activity) {
        val window = activity.window
        window.statusBarColor = Violet600.toArgb()
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        onDispose { }
    }
}
