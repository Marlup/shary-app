// --- LoginScreen.kt ---
package com.shary.app.ui.screens.login

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Palette
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
import com.shary.app.core.domain.types.enums.AppTheme
import com.shary.app.ui.screens.home.utils.Screen
import com.shary.app.ui.screens.utils.LoadingOverlay
import com.shary.app.ui.screens.utils.PasswordTextField
import com.shary.app.viewmodels.authentication.AuthenticationMode
import com.shary.app.viewmodels.authentication.AuthenticationViewModel
import kotlinx.coroutines.launch

// Optional: small Hilt entry point if you still want to check registration on the cloud
/*
@EntryPoint
@InstallIn(SingletonComponent::class)
interface LoginScreenEntryPoints {
    fun cloudService(): CloudServiceImpl
}
 */

/**
 * UI should only react to states and events from the ViewModel, being
 * agnostic (not knowing) about services and dependencies of the data layer.
 * */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavHostController,
    onThemeChosen: (AppTheme) -> Unit = {} // optional callback for theme switching
) {
    // ---------------- ViewModels ----------------
    val authenticationViewModel: AuthenticationViewModel = hiltViewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ---------------- States ----------------
    val loginForm by authenticationViewModel.logForm.collectAsState()
    val loading by authenticationViewModel.loading.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // ---------------- Init ----------------
    LaunchedEffect(Unit) { authenticationViewModel.setMode(AuthenticationMode.LOGIN) }

    // ---------------- Events ----------------
    LaunchedEffect(Unit) {
        authenticationViewModel.events.collect { ev ->
            when (ev) {
                is AuthenticationEvent.Success -> {
                    scope.launch { authenticationViewModel.onLoginSuccess(loginForm.username) }
                    navController.navigate(Screen.Fields.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
                is AuthenticationEvent.CloudSignedOut -> {
                    Toast.makeText(context, "Signed out", Toast.LENGTH_SHORT).show()
                    navController.navigate(Screen.Logup.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
                is AuthenticationEvent.Error -> snackbarHostState.showSnackbar(ev.message)
                else -> Unit
            }
        }
    }

    // ---------------- Lifecycle cleanup ----------------
    val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)
    DisposableEffect(lifecycleOwner.value) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) authenticationViewModel.clearLogFormStates()
        }
        val lifecycle = lifecycleOwner.value.lifecycle
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    // ---------------- Theme Menu (TopRight) + Logout (TopLeft) ----------------
    var expanded by remember { mutableStateOf(false) }
    var selectedTheme by remember { mutableStateOf(AppTheme.Pastel) }


    LoadingOverlay(isLoading = loading) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(

                    title = {
                        Text(
                            text = "Shary",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            authenticationViewModel.signOutCloud()
                            navController.navigate(Screen.Logup.route)
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Logout"
                            )
                        }
                    },
                    actions = {
                        // Theme menu button
                        Box {
                            IconButton(onClick = { expanded = true }) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = "Choose Theme"
                                )
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                AppTheme.entries.forEach { theme ->
                                    DropdownMenuItem(
                                        text = { Text(theme.name) },
                                        onClick = {
                                            selectedTheme = theme
                                            expanded = false
                                            onThemeChosen(theme)
                                        }
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    )
                )
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
                    value = loginForm.username,
                    onValueChange = { authenticationViewModel.updateUsername(it) },
                    label = { Text("Username") },
                    singleLine = true,
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                PasswordTextField(
                    password = loginForm.password,
                    onPasswordChange = { authenticationViewModel.updatePassword(it) },
                    enabled = !loading
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { authenticationViewModel.submit(context) },
                    enabled = !loading,
                    modifier = Modifier.size(200.dp, 50.dp)
                ) {
                    Text(if (loading) "Checking..." else "Login")
                }
            }
        }
    }
}
