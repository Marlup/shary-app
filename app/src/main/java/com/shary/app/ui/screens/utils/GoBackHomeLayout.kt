package com.shary.app.ui.screens.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.shary.app.ui.screens.home.utils.Screen


@Composable
fun GoToScreen(
    navController: NavHostController,
    targetScreen: Screen,
    onExit: () -> Unit
) {
    Button(onClick = {
        onExit()
        navController.navigate(targetScreen.route) {
            popUpTo(targetScreen.route) { inclusive = true }
            launchSingleTop = true
        }
    }) {
        Icon(
            Icons.Filled.Home,
            contentDescription = "Go to Screen button"
        )
    }
}

@Composable
fun GoBackButton(navController: NavHostController) {
    Button(onClick = {
        navController.navigate(Screen.Home.route) {
            popUpTo(Screen.Home.route) { inclusive = true }
            launchSingleTop = true
        }
    }) {
        Icon(
            Icons.Filled.Home,
            contentDescription = "Go back button"
        )
    }
}
