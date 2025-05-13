// --- LoginScreen.kt ---

package com.shary.app.ui.screens.login

import android.util.Log
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.shary.app.core.Session
import com.shary.app.core.dependencyContainer.DependencyContainer
import com.shary.app.services.cloud.CloudService
import com.shary.app.ui.screens.home.utils.Screen
import com.shary.app.utils.BiometricAuthManager
import com.shary.app.utils.ValidationUtils.validateLoginCredentials
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavHostController,
    session: Session,
    cloudService: CloudService
) {
    val context = LocalContext.current
    val activity = context as FragmentActivity //
    //val executor = remember { ContextCompat.getMainExecutor(context) }
    val scope = rememberCoroutineScope()
    val errorMessage by remember { mutableStateOf<String?>(null) }

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }

    fun showError(message: String) {
        scope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }

    // ⚠️ Clear dependencies when this screen loads
    LaunchedEffect(Unit) {
        DependencyContainer.initAll(context)
    }

    fun clearStates() {
        username = ""
        password = ""
    }

    val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)

    DisposableEffect(lifecycleOwner.value) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                clearStates()
            }
        }

        val lifecycle = lifecycleOwner.value.lifecycle
        lifecycle.addObserver(observer)

        onDispose {
            lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Login") })
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
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val message = validateLoginCredentials(username, password)
                    if (message.isBlank()) {
                        // Generate keys on the fly
                        session.generateKeys(password, username)

                        // Try to login (check credentials)
                        if (session.tryLogin(context, username, password)) {
                            scope.launch {
                                val email = session.email.toString()

                                if (cloudService.isUserRegistered(email)) {
                                    Toast.makeText(
                                        context,
                                        "User was registered on Cloud ",
                                        Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        "User was not registered on Cloud ",
                                        Toast.LENGTH_SHORT).show()
                                }
                            }
                            navController.navigate(Screen.Home.route)
                        } else {
                            Toast.makeText(context, "Invalid credentials", Toast.LENGTH_SHORT).show()
                        }
                    }
                    else {
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    //.fillMaxWidth(0.5f)
                    .size(200.dp, 50.dp)
            ) {
                Text("Login")
            }

            Spacer(modifier = Modifier.height(32.dp))

            // UI: button for launching authentication
            Column(
                //modifier = Modifier.size(200.dp, 50.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Button(onClick = {
                    Log.d("Biometric", "Entering biometric")
                    val biometricManager = BiometricAuthManager(
                        context = context,
                        activity = activity,
                        onAuthSuccess = {
                            // 🔓 Successful Authentication → Continue
                            navController.navigate("Home")
                        },
                    )

                    val error = biometricManager.authenticate()
                    if (error != null) {
                        showError(error)
                    }
                },
                    modifier = Modifier.size(200.dp, 50.dp),
                ) {
                    Text("Biometric Login")
                }

                errorMessage?.let {
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(text = it, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
