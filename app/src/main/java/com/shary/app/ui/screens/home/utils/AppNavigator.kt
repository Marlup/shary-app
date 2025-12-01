package com.shary.app.ui.screens.home.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
//import androidx.hilt.navigation.compose.hiltViewModel // deprecated location of hiltViewModel
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.shary.app.core.domain.interfaces.navigator.CredentialsEntryPoint
import com.shary.app.core.domain.interfaces.persistance.CredentialsStore
import com.shary.app.core.domain.types.enums.AppTheme
import com.shary.app.core.domain.types.enums.StartDestination
import com.shary.app.ui.screens.field.FieldsScreen
import com.shary.app.ui.screens.fileVisualizer.FileVisualizerScreen
import com.shary.app.ui.screens.login.LoginScreen
import com.shary.app.ui.screens.logup.LogupScreen
import com.shary.app.ui.screens.request.RequestsScreen
import com.shary.app.ui.screens.summary.SummaryScreen
import com.shary.app.ui.screens.user.UsersScreen
import com.shary.app.viewmodels.authentication.AuthenticationViewModel
import dagger.hilt.EntryPoints


@Composable
fun AppNavigator(onThemeSelected: (AppTheme) -> Unit) {
    val navController = rememberNavController()

    // ✅ ViewModel through Hilt
    val authModel: AuthenticationViewModel = hiltViewModel()

    // ✅ Non-VM dependency via EntryPoint
    val context = LocalContext.current
    val creds: CredentialsStore = remember {
        val ep = EntryPoints.get(context.applicationContext, CredentialsEntryPoint::class.java)
        ep.credentialsStore()
    }

    // Decide start destination (replace this with your existing resolver if you like)
    val startDestination = remember {
        // Example logic: if credentials exist → HOME; if signature exists but no creds → LOGIN; else LOGUP
        val hasCreds = creds.hasCredentials(context)
        when {
            hasCreds -> StartDestination.LOGIN
            authModel.isSignatureActive(context) -> StartDestination.LOGIN
            else -> StartDestination.LOGUP
        }
    }

    NavHost(
        navController = navController,
        startDestination = when (startDestination) {
            StartDestination.LOGIN -> Screen.Login.route
            StartDestination.LOGUP -> Screen.Logup.route
        }
    ) {
        composable(Screen.Logup.route) { LogupScreen(navController) }
        composable(Screen.Login.route) { LoginScreen(navController, onThemeSelected) }
        //composable(Screen.Home.route) { HomeScreen(navController) }
        composable(Screen.Summary.route) { SummaryScreen(navController) }
        composable(Screen.Fields.route) { FieldsScreen(navController) }
        composable(Screen.Users.route) { UsersScreen(navController) }
        composable(Screen.Requests.route) { RequestsScreen(navController) }
        composable(Screen.FileVisualizer.route) { FileVisualizerScreen(navController) }
    }
}
