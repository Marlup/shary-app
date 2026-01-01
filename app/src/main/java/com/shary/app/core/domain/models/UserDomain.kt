package com.shary.app.core.domain.models

import com.shary.app.core.domain.models.RequestDomain.Companion.initialize
import com.shary.app.core.domain.types.enums.FieldAttribute
import com.shary.app.core.domain.types.enums.UserAttribute
import com.shary.app.utils.Validation.validateEmailSyntax
import java.time.Instant
import kotlin.String


// --------------------
// User (Domain Model)
// --------------------
data class UserDomain(
    var username: String = "",
    var email: String = "",
    val dateAdded: Instant = Instant.now()
) {
    companion object {
        fun UserDomain?.orEmpty(): UserDomain =
            (this ?: initialize()) as UserDomain

        fun create(
            username: String = "",
            email: String = "",
        ): UserDomain {
            return UserDomain(
                username = username,
                email = email,
                dateAdded = Instant.now()
            )
        }
    }

    val isEmailValid: Boolean
        get() = validateEmailSyntax(email).isEmpty()

    fun matchBy(criteria: String, searchBy: UserAttribute): Boolean {
        return when (searchBy) {
            UserAttribute.Username -> username.contains(criteria, ignoreCase = true)
            UserAttribute.Email -> email.orEmpty().contains(criteria, ignoreCase = true)
        }
    }
}

fun UserDomain.reset(): UserDomain = this.copy(
    username = "",
    email = "",
    dateAdded = Instant.EPOCH
)