package com.shary.app.core.domain.interfaces.services

import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.types.enums.StatusDataSentDb

interface CloudService {
    suspend fun sendPing(): Boolean
    suspend fun isUserRegistered(user: String): Boolean
    suspend fun uploadUser(user: String): String
    suspend fun deleteUser(user: String): Boolean
    suspend fun uploadData(
        fields: List<FieldDomain>,
        user: String,
        consumers: List<String>,
        isRequest: Boolean = false
    ): Map<String, StatusDataSentDb>
    suspend fun getPubKey(userHash: String): String?
}