package com.shary.app.ui.screens.home.utils

sealed class Screen(val route: String) {
    object Logup : Screen("logup")
    object Login : Screen("login")
    object Home : Screen("home")
    object Fields : Screen("fields")
    object Users : Screen("users")
    object Requests : Screen("requests")
    object FileVisualizer : Screen("file_visualizer")
}
