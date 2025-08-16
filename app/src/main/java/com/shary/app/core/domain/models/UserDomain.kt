package com.shary.app.core.domain.models

import com.shary.app.utils.Validation.validateEmailSyntax
import java.time.Instant


// --------------------
// User (Domain Model)
// --------------------
data class UserDomain(
    val username: String,
    val email: String,
    val dateAdded: Instant
) {
    val isEmailValid: Boolean
        get() = validateEmailSyntax(email).isEmpty()
}
