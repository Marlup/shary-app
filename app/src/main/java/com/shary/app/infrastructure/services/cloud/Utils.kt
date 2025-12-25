package com.shary.app.infrastructure.services.cloud

import com.shary.app.core.domain.types.enums.StatusDataSentDb
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

object Utils {

    fun authBearerHeader(token: String?): Map<String, String> {
        return if (token != null) mapOf("Authorization" to "Bearer $token")
        else emptyMap()
    }

    fun buildPostRequest(
        url: String,
        jsonPayload: String,
        headers: Map<String, String> = emptyMap()
    ): Request {
        val requestBody = jsonPayload.toRequestBody("application/json".toMediaType())
        return Request.Builder()
            .url(url)
            .post(requestBody)
            .headers(headers.toHeaders())
            .build()
    }


    fun evaluateStatusCode(code: Int): StatusDataSentDb {
        return when (code) {
            200 -> StatusDataSentDb.SUCCESS
            201 -> StatusDataSentDb.STORED
            400 -> StatusDataSentDb.MISSING_FIELD
            401, 403 -> StatusDataSentDb.FORBIDDEN
            404 -> StatusDataSentDb.NOT_FOUND
            409 -> StatusDataSentDb.EXISTS
            else -> StatusDataSentDb.ERROR
        }
    }
}
