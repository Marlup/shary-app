package com.shary.app.ui.screens.home.utils

sealed class Screen(val route: String) {
    //data object Start : Screen("start")          // splash / resolver
    // Compatibility alias kept while legacy calls are migrated.
    data object Logup : Screen("logup")
    data object Signup : Screen("signup")
    data object Login : Screen("login") {
        const val PASSWORD_CHANGED_ARG = "passwordChanged"
        const val routePattern = "login?$PASSWORD_CHANGED_ARG={$PASSWORD_CHANGED_ARG}"

        fun routeWithPasswordChanged(passwordChanged: Boolean): String =
            "login?$PASSWORD_CHANGED_ARG=$passwordChanged"
    }
    data object Home : Screen("home")
    data object SummaryRequest : Screen("summary_request") {
        const val REQUEST_ID_ARG = "requestId"
        const val routePattern = "summary_request?$REQUEST_ID_ARG={$REQUEST_ID_ARG}"

        fun reviewRoute(requestId: String): String =
            "summary_request?$REQUEST_ID_ARG=$requestId"
    }
    data object SummaryField : Screen("summary_field") {
        const val RESPONSE_REQUEST_ID_ARG = "responseRequestId"
        const val routePattern = "summary_field?$RESPONSE_REQUEST_ID_ARG={$RESPONSE_REQUEST_ID_ARG}"

        fun responseRoute(requestId: String): String =
            "summary_field?$RESPONSE_REQUEST_ID_ARG=$requestId"
    }
    data object Fields : Screen("fields")
    data object Connectors : Screen("connectors")
    data object Settings : Screen("settings")
    data object Users : Screen("users")
    data object Requests : Screen("requests")
}
