package com.shary.app.core.domain.interfaces.services

import android.content.Intent
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.models.UserDomain

interface EmailService {
    suspend fun sendRequestFile(
        fields: List<FieldDomain>,
        recipients: List<UserDomain>
    ): Result<Intent>

    suspend fun sendResponseFile(
        fields: List<FieldDomain>,
        recipients: List<UserDomain>
    ): Result<Intent>
}