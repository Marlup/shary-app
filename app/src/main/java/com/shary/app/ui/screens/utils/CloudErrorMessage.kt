package com.shary.app.ui.screens.utils

import com.shary.app.infrastructure.services.cloud.IdentityBackendRejectedException
import com.shary.app.infrastructure.services.cloud.IdentityEmailMismatchException
import com.shary.app.infrastructure.services.cloud.VerifiedEmailRequiredException
import com.shary.app.infrastructure.services.cloud.CloudBackendApiException
import com.shary.app.infrastructure.services.cloud.CloudRateLimitedException
import com.shary.app.infrastructure.services.cloud.CloudUnsupportedSchemaVersionException
import java.io.IOException

fun cloudErrorMessage(throwable: Throwable?): String {
    if (throwable == null) return "Cloud operation failed."

    return when (throwable) {
        is VerifiedEmailRequiredException ->
            "Verify your email address before using cloud identity features."
        is IdentityEmailMismatchException ->
            "Signed-in email does not match the selected owner identity."
        is IdentityBackendRejectedException ->
            throwable.message ?: "Identity operation was rejected by backend."
        is CloudUnsupportedSchemaVersionException ->
            "Cloud contract mismatch detected. Update the app before retrying."
        is CloudRateLimitedException -> {
            val retry = throwable.retryAfterSeconds
            if (retry != null && retry > 0) {
                "Cloud is rate-limited. Try again in ${retry}s."
            } else {
                "Cloud is rate-limited. Please retry shortly."
            }
        }
        is CloudBackendApiException -> {
            val code = throwable.errorCode
            when {
                code.equals("rate_limited", ignoreCase = true) ->
                    "Cloud is rate-limited. Please retry shortly."
                code.equals("unsupported_schema_version", ignoreCase = true) ->
                    "Cloud contract mismatch detected. Update the app before retrying."
                code.equals("decision_conflict", ignoreCase = true) ->
                    "This cloud item was already decided. Refresh the inbox before retrying."
                code.equals("stale_token", ignoreCase = true) ||
                    code.equals("token_expired", ignoreCase = true) ||
                    throwable.statusCode == 401 ||
                    throwable.statusCode == 403 ->
                    "Cloud session expired or was rejected. Sign in again, then retry."
                !code.isNullOrBlank() ->
                    "Cloud request failed ($code)."
                else ->
                    throwable.message ?: "Cloud request failed."
            }
        }
        is IOException ->
            "Cloud service is unreachable. Check your connection and try again."
        else ->
            throwable.message?.takeIf { it.isNotBlank() } ?: "Cloud operation failed."
    }
}
