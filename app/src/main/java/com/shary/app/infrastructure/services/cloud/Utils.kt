package com.shary.app.infrastructure.services.cloud

import com.shary.app.core.domain.types.enums.StatusDataSentDb
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

object Utils {
    fun authHeader(token: String): Map<String, String> =
        mapOf("Authorization" to "Bearer $token")

    fun buildPostRequest(
        url: String,
        body: Map<String, String>,
        headers: Map<String, String> = emptyMap()
    ): Request {
        val json = Json.encodeToString(body)
        val requestBody = json.toRequestBody("application/json; charset=utf-8".toMediaType())

        val builder = Request.Builder()
            .url(url)
            .post(requestBody)

        headers.forEach { (key, value) ->
            builder.addHeader(key, value)
        }

        return builder.build()
    }

    fun evaluateStatusCode(code: Int): StatusDataSentDb = when (code) {
        200 -> StatusDataSentDb.STORED
        400 -> StatusDataSentDb.MISSING_FIELD
        409 -> StatusDataSentDb.EXISTS
        500 -> StatusDataSentDb.ERROR
        else -> StatusDataSentDb.ERROR
    }
}
