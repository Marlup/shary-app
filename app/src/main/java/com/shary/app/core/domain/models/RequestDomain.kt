package com.shary.app.core.domain.models

import java.time.Instant

data class RequestDomain(
    val fields: List<FieldDomain>,
    val sender: UserDomain,
    val recipients: List<UserDomain>,
    val dateAdded: Instant
)
