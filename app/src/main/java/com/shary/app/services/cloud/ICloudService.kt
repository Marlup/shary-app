package com.shary.app.services.cloud

import com.shary.app.Field
import com.shary.app.core.enums.StatusDataSentDb

interface ICloudService {
    suspend fun sendPing(): Boolean
    suspend fun isUserRegistered(user: String): Boolean
    suspend fun uploadUser(user: String): Pair<Boolean, String>
    suspend fun deleteUser(user: String): Boolean
    suspend fun uploadData(
        fields: List<Field>,
        user: String,
        consumers: List<String>,
        onRequest: Boolean = false
    ): Map<String, StatusDataSentDb>
    suspend fun getPubKey(userHash: String): String?



}