// --- Validation.kt ---

package com.shary.app.utils

object Validation {

    fun validatePassword(password: String): String {
        if (password.length < 8) {
            return "Password must be at least 8 characters long."
        }
        if (!password.any { it.isUpperCase() }) {
            return "Password must contain at least one uppercase letter."
        }
        if (!password.any { it.isLowerCase() }) {
            return "Password must contain at least one lowercase letter."
        }
        if (!password.any { it.isDigit() }) {
            return "Password must contain at least one number."
        }
        if (!Regex("[!@#\$%^&*(),.?\":{}|<>]").containsMatchIn(password)) {
            return "Password must contain at least one special character (!@#\$%^&*...)."
        }

        return ""
    }

    fun validateEmailSyntax(email: String): String {
        if (email.isBlank()) {
            return "Email cannot be empty."
        }
        if (!email.contains("@")) {
            return "Unexpected email format: @?."
        }
        if (!email.contains(".")) {
            return "Unexpected email format: .?."
        }
        if (email.startsWith("@")) {
            return "Unexpected email format: starts with @."
        }

        return ""
    }

    fun validateLogupCredentials(
        email: String,
        username: String,
        password: String,
        confirmPassword: String
    ): String {
        val emailMessage = validateEmailSyntax(email)
        if (emailMessage.isNotEmpty()) {
            return emailMessage
        }

        if (username.isEmpty()) {
            return "Username cannot be empty"
        }

        if (password != confirmPassword) {
            return "Passwords do not match"
        }

        return validatePassword(password)
    }
    fun validateLoginCredentials(
        username: String,
        password: String,
    ): String {
        if (username.isBlank()) {
            return "Username cannot be empty"
        }

        return validatePassword(password)
    }
}
