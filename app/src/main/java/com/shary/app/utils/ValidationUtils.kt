// --- ValidationUtils.kt ---

package com.shary.app.utils

object ValidationUtils {

    fun validatePassword(password: String): Pair<Boolean, String> {
        if (password.length < 8) {
            return false to "Password must be at least 8 characters long."
        }
        if (!password.any { it.isUpperCase() }) {
            return false to "Password must contain at least one uppercase letter."
        }
        if (!password.any { it.isLowerCase() }) {
            return false to "Password must contain at least one lowercase letter."
        }
        if (!password.any { it.isDigit() }) {
            return false to "Password must contain at least one number."
        }
        if (!Regex("[!@#\$%^&*(),.?\":{}|<>]").containsMatchIn(password)) {
            return false to "Password must contain at least one special character (!@#\$%^&*...)."
        }

        return true to ""
    }

    fun validateEmailSyntax(email: String): Pair<Boolean, String> {
        if (email.isBlank()) {
            return false to "Email cannot be empty."
        }
        if (!email.contains("@")) {
            return false to "Unexpected email format: @?."
        }
        if (email.startsWith("@")) {
            return false to "Unexpected email format: starts with @."
        }

        return true to ""
    }

    fun validateLogupCredentials(
        email: String,
        username: String,
        password: String,
        confirmPassword: String
    ): String {
        val emailValidation = validateEmailSyntax(email)
        if (!emailValidation.first) {
            return emailValidation.second
        }

        if (username.isBlank()) {
            return "Username cannot be empty"
        }

        val passwordValidation = validatePassword(password)
        if (!passwordValidation.first) {
            return passwordValidation.second
        }

        if (password != confirmPassword) {
            return "Passwords do not match"
        }
        return ""
    }
    fun validateLoginCredentials(
        username: String,
        password: String,
    ): String {
        if (username.isBlank()) {
            return "Username cannot be empty"
        }

        val passwordValidation = validatePassword(password)
        if (!passwordValidation.first) {
            return passwordValidation.second
        }

        return ""
    }
}
