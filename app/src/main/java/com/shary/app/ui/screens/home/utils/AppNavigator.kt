package com.shary.app.ui.screens.home.utils

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.shary.app.core.domain.interfaces.navigator.CredentialsEntryPoint
import com.shary.app.core.domain.interfaces.persistance.CredentialsStore
import com.shary.app.core.domain.interfaces.security.CryptographyManager
import com.shary.app.core.domain.types.enums.AppTheme
import com.shary.app.core.domain.types.enums.StartDestination
import com.shary.app.ui.connector.ConnectorsScreen
import com.shary.app.ui.screens.field.FieldsScreen
import com.shary.app.ui.screens.login.LoginScreen
import com.shary.app.ui.screens.logup.LogupScreen
import com.shary.app.ui.screens.request.RequestsScreen
import com.shary.app.ui.screens.settings.SettingsScreen
import com.shary.app.ui.screens.summary.SummaryFieldScreen
import com.shary.app.ui.screens.summary.SummaryRequestScreen
import com.shary.app.ui.screens.user.UsersScreen
import com.shary.app.viewmodels.authentication.AuthenticationViewModel
import com.shary.app.viewmodels.authentication.SessionViewModel
import com.shary.app.viewmodels.communication.CloudViewModel
import dagger.hilt.EntryPoints


@Composable
fun AppNavigator(onThemeSelected: (AppTheme) -> Unit) {
    val navController = rememberNavController()

    // ✅ ViewModel through Hilt
    val authModel: AuthenticationViewModel = hiltViewModel()
    val sessionViewModel: SessionViewModel = hiltViewModel()
    val sessionClosing by sessionViewModel.sessionClosing.collectAsState()
    val cloudViewModel: CloudViewModel = hiltViewModel()

    // ✅ Non-VM dependency via EntryPoint
    val context = LocalContext.current
    val creds: CredentialsStore = remember {
        val ep = EntryPoints.get(context.applicationContext, CredentialsEntryPoint::class.java)
        ep.credentialsStore()
    }
    val crypto: CryptographyManager = remember {
        val ep = EntryPoints.get(context.applicationContext, CredentialsEntryPoint::class.java)
        ep.cryptographyManager()
    }

    // Decide start destination (replace this with your existing resolver if you like)
    val startDestination = remember {
        // Example logic: if credentials exist → HOME; if signature exists but no creds → LOGIN; else LOGUP
        val hasCreds = creds.hasCredentials(context)
        val hasUsableCreds = if (hasCreds) creds.hasUsableCredentials(context, crypto) else false
        when {
            hasUsableCreds -> StartDestination.LOGIN
            authModel.isSignatureActive(context) -> StartDestination.LOGIN
            else -> StartDestination.LOGUP
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        cloudViewModel.refreshOnlineStatus()
    }

    androidx.compose.runtime.LaunchedEffect(sessionViewModel) {
        sessionViewModel.sessionClosed.collect {
            navController.navigate(Screen.Login.route) {
                popUpTo(navController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    Box {
        NavHost(
            navController = navController,
            startDestination = when (startDestination) {
                StartDestination.LOGIN -> Screen.Login.route
                StartDestination.LOGUP -> Screen.Signup.route
            },
            enterTransition = {
                fadeIn(animationSpec = tween(220)) +
                        slideInVertically(animationSpec = tween(220), initialOffsetY = { it / 20 })
            },
            exitTransition = { fadeOut(animationSpec = tween(180)) },
            popEnterTransition = {
                fadeIn(animationSpec = tween(220)) +
                        slideInVertically(animationSpec = tween(220), initialOffsetY = { it / 20 })
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(180)) +
                        slideOutVertically(animationSpec = tween(180), targetOffsetY = { it / 20 })
            }
        ) {
        composable(Screen.Signup.route) { LogupScreen(navController) }
        composable(Screen.Logup.route) { LogupScreen(navController) }
        composable(
            route = Screen.Login.routePattern,
            arguments = listOf(
                navArgument(Screen.Login.PASSWORD_CHANGED_ARG) {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val passwordChanged =
                backStackEntry.arguments?.getBoolean(Screen.Login.PASSWORD_CHANGED_ARG) ?: false
            LoginScreen(navController, onThemeSelected, passwordChanged)
        }
        composable(
            route = Screen.SummaryRequest.routePattern,
            arguments = listOf(
                navArgument(Screen.SummaryRequest.REQUEST_ID_ARG) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            SummaryRequestScreen(
                navController = navController,
                draftRequestId = backStackEntry.arguments
                    ?.getString(Screen.SummaryRequest.REQUEST_ID_ARG)
            )
        }
        composable(
            route = Screen.SummaryField.routePattern,
            arguments = listOf(
                navArgument(Screen.SummaryField.RESPONSE_REQUEST_ID_ARG) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            SummaryFieldScreen(
                navController = navController,
                responseRequestId = backStackEntry.arguments
                    ?.getString(Screen.SummaryField.RESPONSE_REQUEST_ID_ARG)
            )
        }
        composable(Screen.Fields.route) {
            FieldsScreen(navController, onLogout = sessionViewModel::closeSession)
        }
        composable(Screen.Connectors.route) {
            ConnectorsScreen(navController, onLogout = sessionViewModel::closeSession)
        }
        composable(Screen.Settings.route) { SettingsScreen(navController) }
        composable(Screen.Users.route) {
            UsersScreen(navController, onLogout = sessionViewModel::closeSession)
        }
        composable(Screen.Requests.route) {
            RequestsScreen(navController, onLogout = sessionViewModel::closeSession)
        }
        }
        if (sessionClosing) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surface
            ) {}
        }
    }
}
