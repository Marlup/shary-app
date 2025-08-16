package com.shary.app.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.shary.app.core.domain.interfaces.persistance.CredentialsStore
import com.shary.app.core.domain.types.enums.StartDestination
import com.shary.app.ui.screens.field.FieldsScreen
import com.shary.app.ui.screens.fileVisualizer.FileVisualizerScreen
import com.shary.app.ui.screens.home.utils.Screen
import com.shary.app.ui.screens.home.utils.StartDestinationResolver
import com.shary.app.ui.screens.login.LoginScreen
import com.shary.app.ui.screens.logup.LogupScreen
import com.shary.app.ui.screens.request.RequestsScreen
import com.shary.app.ui.screens.user.UsersScreen
import com.shary.app.viewmodels.authentication.AuthenticationViewModel
import dagger.hilt.EntryPoint

@Composable
fun AppNavigator(
    authModel: AuthenticationViewModel = hiltViewModel(), // o EntryPoint si no quieres VM
    credentialsStore: CredentialsStore = hiltViewModel() // idem
) {
    val navController = rememberNavController()

    // Resolución directa sin StateFlow
    val ctx = LocalContext.current
    val startDestination = remember {
        StartDestinationResolver.resolve(ctx, authModel, credentialsStore)
    }

    NavHost(
        navController = navController,
        startDestination = when (startDestination) {
            StartDestination.HOME -> Screen.Home.route
            StartDestination.LOGIN -> Screen.Login.route
            StartDestination.LOGUP -> Screen.Logup.route
        }
    ) {
        composable(Screen.Logup.route) { LogupScreen(navController) }
        composable(Screen.Login.route) { LoginScreen(navController) }
        composable(Screen.Home.route) { HomeScreen(navController) }
        composable(Screen.Fields.route) { FieldsScreen(navController) }
        composable(Screen.Users.route) { UsersScreen(navController) }
        composable(Screen.Requests.route) { RequestsScreen(navController) }
        composable(Screen.FileVisualizer.route) { FileVisualizerScreen(navController) }
    }
}
