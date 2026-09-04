package com.shary.app.infrastructure.services.cloud

sealed class CloudIdentityException(message: String) : SecurityException(message)

class VerifiedEmailRequiredException :
    CloudIdentityException("Email verification is required before using cloud identity features.")

class IdentityEmailMismatchException :
    CloudIdentityException("Signed-in email does not match the requested identity.")

class IdentityBackendRejectedException(message: String) : CloudIdentityException(message)

class CloudUnsupportedSchemaVersionException(
    message: String = "Unsupported backend schema version. Client update required.",
    val requestId: String? = null
) : IllegalStateException(message)

class CloudRateLimitedException(
    val retryAfterSeconds: Long? = null,
    val requestId: String? = null,
    message: String = "Cloud request was rate-limited."
) : IllegalStateException(message)

class CloudBackendApiException(
    val statusCode: Int,
    val errorCode: String? = null,
    val requestId: String? = null,
    message: String
) : IllegalStateException(message)
