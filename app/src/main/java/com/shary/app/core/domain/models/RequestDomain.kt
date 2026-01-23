package com.shary.app.core.domain.models

import java.time.Instant
import com.shary.app.core.domain.models.RequestDomain.Companion.initialize


data class RequestDomain(
    val fields: List<FieldDomain>,
    val user: String,
    val recipients: List<String>,
    val dateAdded: Instant,
    val owned: Boolean,
    val responded: Boolean
) {

    companion object {
        fun RequestDomain?.orEmpty(): RequestDomain =
            this ?: initialize()

        fun create(
            fields: List<FieldDomain>,
            user: String,
            recipients: List<String> = emptyList(),
        ): RequestDomain {
            return RequestDomain(
                fields = fields,
                user = user,
                recipients = recipients,
                dateAdded = Instant.now(),
                owned = true,
                responded = false
            )
        }

        fun initialize(): RequestDomain = RequestDomain(
            fields = emptyList(),
            user = String(),
            recipients = emptyList(),
            dateAdded = Instant.now(),
            owned = false,
            responded = false
        )
    }
}

fun RequestDomain?.orEmpty(): RequestDomain =
    this ?: initialize()

fun RequestDomain.reset(): RequestDomain = this.copy(
    fields = emptyList(),
    user = String(),
    recipients = emptyList(),
    dateAdded = Instant.EPOCH,
    owned = false,
    responded = false
)
