package com.shary.app.core.domain.models

import com.shary.app.core.domain.types.enums.FieldAttribute
import com.shary.app.core.domain.types.enums.UserAttribute
import com.shary.app.utils.Validation.validateEmailSyntax
import java.time.Instant


// --------------------
// User (Domain Model)
// --------------------
data class UserDomain(
    var username: String = "",
    var email: String = "",
    val dateAdded: Instant = Instant.now()
) {
    val isEmailValid: Boolean
        get() = validateEmailSyntax(email).isEmpty()

    fun matchBy(criteria: String, searchBy: UserAttribute): Boolean {
        return when (searchBy) {
            UserAttribute.Username -> username.contains(criteria, ignoreCase = true)
            UserAttribute.Email -> email.orEmpty().contains(criteria, ignoreCase = true)
        }
    }
}
