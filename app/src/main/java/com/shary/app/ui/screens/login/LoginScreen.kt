package com.shary.app.ui.screens.login

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.shary.app.core.domain.interfaces.viewmodels.AuthenticationEvent
import com.shary.app.core.domain.types.enums.AppTheme
import com.shary.app.ui.components.SharyPrimaryButton
import com.shary.app.ui.components.SharyTextButton
import com.shary.app.ui.screens.home.utils.Screen
import com.shary.app.ui.screens.utils.LoadingOverlay
import com.shary.app.ui.screens.utils.PasswordTextField
import com.shary.app.ui.theme.SharyRadius
import com.shary.app.ui.theme.SurfaceLight
import com.shary.app.ui.theme.Violet600
import com.shary.app.viewmodels.authentication.AuthenticationMode
import com.shary.app.viewmodels.authentication.AuthenticationViewModel
import com.shary.app.viewmodels.configuration.SettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    navController: NavHostController,
    onThemeChosen: (AppTheme) -> Unit = {},
    passwordChanged: Boolean = false
) {
    val authenticationViewModel: AuthenticationViewModel = hiltViewModel()
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    val loginForm by authenticationViewModel.logForm.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()
    val loading by authenticationViewModel.loading.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit, settings.rememberEmailAtLogin, settings.rememberedEmail) {
        authenticationViewModel.setMode(AuthenticationMode.LOGIN)
        val rememberedEmail = settings.rememberedEmail
        if (rememberedEmail.isNotBlank() && loginForm.email.isBlank()) {
            authenticationViewModel.updateEmail(rememberedEmail)
        }
    }

    LaunchedEffect(passwordChanged) {
        if (passwordChanged) {
            snackbarHostState.showSnackbar("Password changed. Please sign in again.")
        }
    }

    LaunchedEffect(Unit) {
        authenticationViewModel.events.collect { ev ->
            when (ev) {
                is AuthenticationEvent.Success -> {
                    scope.launch {
                        settingsViewModel.rememberEmailIfEnabled(loginForm.email)
                        authenticationViewModel.onLoginSuccess(loginForm.email)
                    }
                    navController.navigate(Screen.Fields.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
                is AuthenticationEvent.CloudSignedOut -> {
                    Toast.makeText(context, "Signed out", Toast.LENGTH_SHORT).show()
                    navController.navigate(Screen.Signup.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
                is AuthenticationEvent.Error -> snackbarHostState.showSnackbar(ev.message)
                else -> Unit
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) authenticationViewModel.clearLogFormStates()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    ApplyAuthStatusBar()

    LoadingOverlay(isLoading = loading) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SurfaceLight)
        ) {
            AuthHero(
                title = "Shary",
                subtitle = "Your data. Your rules.\nShared only when you choose."
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = loginForm.email,
                    onValueChange = authenticationViewModel::updateEmail,
                    label = { Text("Email") },
                    singleLine = true,
                    enabled = !loading,
                    shape = SharyRadius.input,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(14.dp))

                PasswordTextField(
                    password = loginForm.password,
                    onPasswordChange = authenticationViewModel::updatePassword,
                    enabled = !loading
                )

                Spacer(Modifier.height(20.dp))

                SharyPrimaryButton(
                    text = if (loading) "Checking..." else "Sign in",
                    onClick = { authenticationViewModel.submit(context) },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth()
                )

                SharyTextButton(
                    text = "Create account",
                    onClick = { navController.navigate(Screen.Signup.route) },
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        SnackbarHost(hostState = snackbarHostState)
    }
}

@Composable
internal fun AuthHero(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
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
            text = title,
            style = MaterialTheme.typography.displayMedium,
            color = Color.White
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.65f)
        )
    }
}

@Composable
private fun ApplyAuthStatusBar() {
    val context = LocalContext.current
    val activity = context as? Activity ?: return
    DisposableEffect(activity) {
        val window = activity.window
        window.statusBarColor = Violet600.toArgb()
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        onDispose { }
    }
}
