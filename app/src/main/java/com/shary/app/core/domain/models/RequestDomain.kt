package com.shary.app.core.domain.models

import java.time.Instant
import com.shary.app.core.domain.models.RequestDomain.Companion.initialize


data class RequestDomain(
    val fields: List<FieldDomain>,
    val user: UserDomain,
    val recipients: List<UserDomain>,
    val dateAdded: Instant
) {

    companion object {
        fun RequestDomain?.orEmpty(): RequestDomain =
            this ?: initialize()

        fun create(
            fields: List<FieldDomain>,
            user: UserDomain,
            recipients: List<UserDomain> = emptyList(),
        ): RequestDomain {
            return RequestDomain(
                fields = fields,
                user = user,
                recipients = recipients,
                dateAdded = Instant.now()
            )
        }

        fun initialize(): RequestDomain = RequestDomain(
            fields = emptyList(),
            user = UserDomain(),
            recipients = emptyList(),
            dateAdded = Instant.now()
        )
    }
}

fun RequestDomain?.orEmpty(): RequestDomain =
    this ?: initialize()

fun RequestDomain.reset(): RequestDomain = this.copy(
    fields = emptyList(),
    user = UserDomain(),
    recipients = emptyList(),
    dateAdded = Instant.EPOCH
)
