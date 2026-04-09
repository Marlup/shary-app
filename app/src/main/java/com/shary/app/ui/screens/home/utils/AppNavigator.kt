package com.shary.app.ui.screens.home.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.shary.app.core.domain.interfaces.navigator.CredentialsEntryPoint
import com.shary.app.core.domain.interfaces.persistance.CredentialsStore
import com.shary.app.core.domain.types.enums.AppTheme
import com.shary.app.core.domain.types.enums.StartDestination
import com.shary.app.ui.screens.field.FieldsScreen
import com.shary.app.ui.screens.login.LoginScreen
import com.shary.app.ui.screens.logup.LogupScreen
import com.shary.app.ui.screens.request.RequestsScreen
import com.shary.app.ui.screens.summary.SummaryFieldScreen
import com.shary.app.ui.screens.summary.SummaryRequestScreen
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
        composable(Screen.SummaryRequest.route) { SummaryRequestScreen(navController) }
        composable(Screen.SummaryField.route) { SummaryFieldScreen(navController) }
        composable(Screen.Fields.route) { FieldsScreen(navController) }
        composable(Screen.Users.route) { UsersScreen(navController) }
        composable(Screen.Requests.route) { RequestsScreen(navController) }
    }
}
