package com.shary.app.ui.screens.home.utils

sealed class Screen(val route: String) {
    data object Start : Screen("start")          // splash / resolver
    data object Logup : Screen("logup")
    data object Login : Screen("login")
    data object Home : Screen("home")
    data object Fields : Screen("fields")
    data object Users : Screen("users")
    data object Requests : Screen("requests")
    data object FileVisualizer : Screen("file_visualizer")
}
