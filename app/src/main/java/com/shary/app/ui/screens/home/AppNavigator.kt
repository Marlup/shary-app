package com.shary.app.ui.screens.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.traceEventStart
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.shary.app.core.Session
import com.shary.app.core.dependencyContainer.DependencyContainer
import com.shary.app.services.cloud.CloudService
import com.shary.app.ui.screens.fields.FieldsScreen
import com.shary.app.ui.screens.users.UsersScreen
import com.shary.app.ui.screens.fileVisualizer.FileVisualizerScreen
import com.shary.app.ui.screens.login.LoginScreen
import com.shary.app.ui.screens.logup.LogupScreen
import com.shary.app.ui.screens.requests.RequestsScreen

@Composable
fun AppNavigator() {
    val session: Session = DependencyContainer.get("session") as Session
    val cloudService: CloudService = DependencyContainer.get("cloud_service")
    val navController = rememberNavController()
    val context = LocalContext.current

    // Determine the initial screen
    val startDestination = remember {
        if (session.isCredentialsActive(context) && session.isSignatureActive(context)) {
            Screen.Login.route
        } else {
            Screen.Logup.route
        }
    }

    NavHost(navController, startDestination = startDestination) {
        composable(Screen.Logup.route) {
            LogupScreen(navController, session, cloudService)
        }
        composable(Screen.Login.route) {
            LoginScreen(navController, session, cloudService)
        }
        composable(Screen.Home.route) {
            HomeScreen(navController, session)
        }
        composable(Screen.Fields.route) {
            FieldsScreen(navController, DependencyContainer.get("field_service"))
        }
        composable(Screen.Users.route) {
            UsersScreen(navController, DependencyContainer.get("user_service"))
        }
        composable(Screen.Requests.route) {
            RequestsScreen(navController, DependencyContainer.get("requestField_service"))
        }
        composable(Screen.FileVisualizer.route) {
            FileVisualizerScreen(navController, session, DependencyContainer.get("file_service"))
        }
    }
}
