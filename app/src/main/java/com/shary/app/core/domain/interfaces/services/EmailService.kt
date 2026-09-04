package com.shary.app.core.domain.interfaces.services

import android.content.Intent
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.models.UserDomain

data class PreparedEmailShare(
    val attachmentId: String,
    val intent: Intent,
    val expiresAtEpochMillis: Long
)

interface EmailService {
    suspend fun prepareRequestShare(
        fields: List<FieldDomain>,
        recipients: List<UserDomain>
    ): Result<PreparedEmailShare>

    suspend fun prepareResponseShare(
        fields: List<FieldDomain>,
        recipients: List<UserDomain>
    ): Result<PreparedEmailShare>

    suspend fun retainPreparedShare(attachmentId: String): Boolean

    suspend fun discardPreparedShare(attachmentId: String): Boolean
}
