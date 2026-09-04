package com.shary.app.core.domain.models

import com.shary.app.core.domain.models.RequestDomain.Companion.initialize
import com.shary.app.core.domain.types.enums.Tag
import com.shary.app.utils.Validation.validateEmailSyntax
import java.time.Instant
import kotlin.String


// --------------------
// User (Domain Model)
// --------------------
data class UserDomain(
    val username: String = "",
    val email: String = "",
    val dateAdded: Instant = Instant.now(),
    val tag: Tag = Tag.Unknown
) {
    companion object {
        fun UserDomain?.orEmpty(): UserDomain =
            (this ?: initialize()) as UserDomain

        fun create(
            username: String = "",
            email: String = "",
            tag: Tag = Tag.Unknown
        ): UserDomain {
            return UserDomain(
                username = username,
                email = email,
                dateAdded = Instant.now(),
                tag = tag
            )
        }
    }

    val isEmailValid: Boolean
        get() = validateEmailSyntax(email).isEmpty()
}

fun UserDomain.reset(): UserDomain = this.copy(
    username = "",
    email = "",
    dateAdded = Instant.EPOCH,
    tag = Tag.Unknown
)
