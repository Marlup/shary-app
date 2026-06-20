package com.shary.app.infrastructure.connector.mcp

import android.content.Context
import com.shary.app.application.connector.sha256Hex
import com.shary.app.core.domain.interfaces.persistance.CredentialsStore
import com.shary.app.core.domain.interfaces.security.AuthenticationService
import com.shary.app.core.domain.types.valueobjects.ConnectorErrorCodes
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

internal const val MCP_RATE_LIMIT_REQUESTS = 120
internal const val MCP_RATE_LIMIT_WINDOW_SECONDS = 60L
internal const val MCP_RATE_LIMIT_TRACKED_CREDENTIALS = 256

internal sealed interface McpAccessDecision {
    data object Allowed : McpAccessDecision

    data class Denied(
        val code: String,
        val message: String,
        val metadata: Map<String, Any?> = emptyMap()
    ) : McpAccessDecision
}

@Singleton
class McpAccessController internal constructor(
    private val vaultAvailable: () -> Boolean,
    private val rateLimiter: McpConnectorRateLimiter
) {
    @Inject
    constructor(
        @ApplicationContext context: Context,
        credentialsStore: CredentialsStore,
        authenticationService: AuthenticationService,
        rateLimiter: McpConnectorRateLimiter
    ) : this(
        vaultAvailable = {
            val authState = authenticationService.state.value
            !credentialsStore.isCredentialsLocked(context) &&
                authState.email.isNotBlank() &&
                authState.safePassword.isNotBlank()
        },
        rateLimiter = rateLimiter
    )

    internal fun evaluate(headers: Map<String, String>): McpAccessDecision {
        if (!vaultAvailable()) {
            return McpAccessDecision.Denied(
                code = ConnectorErrorCodes.POLICY_DENIED,
                message = "Shary is locked or signed out. Unlock Shary before using connector tools."
            )
        }

        val credentialKey = headers.headerValue("Authorization")
            ?.removePrefix("Bearer")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let(::sha256Hex)
            ?: "missing"
        val rateDecision = rateLimiter.acquire(credentialKey)
        return if (rateDecision.allowed) {
            McpAccessDecision.Allowed
        } else {
            McpAccessDecision.Denied(
                code = ConnectorErrorCodes.RATE_LIMITED,
                message = "Connector request rate limit exceeded.",
                metadata = mapOf("retry_after_seconds" to rateDecision.retryAfterSeconds)
            )
        }
    }
}

internal data class McpRateLimitDecision(
    val allowed: Boolean,
    val retryAfterSeconds: Long = 0
)

@Singleton
class McpConnectorRateLimiter internal constructor(
    private val maxRequests: Int,
    private val windowSeconds: Long,
    private val maxTrackedCredentials: Int,
    private val nowEpochSeconds: () -> Long
) {
    @Inject
    constructor() : this(
        maxRequests = MCP_RATE_LIMIT_REQUESTS,
        windowSeconds = MCP_RATE_LIMIT_WINDOW_SECONDS,
        maxTrackedCredentials = MCP_RATE_LIMIT_TRACKED_CREDENTIALS,
        nowEpochSeconds = { System.currentTimeMillis() / 1000 }
    )

    private data class Window(
        val startedAt: Long,
        val requestCount: Int
    )

    private val windows = ConcurrentHashMap<String, Window>()

    internal fun acquire(credentialKey: String): McpRateLimitDecision {
        val now = nowEpochSeconds()
        val boundedKey = boundedCredentialKey(credentialKey, now)
        var decision = McpRateLimitDecision(allowed = true)
        windows.compute(boundedKey) { _, current ->
            val active = current?.takeIf { now - it.startedAt < windowSeconds }
            when {
                active == null -> Window(startedAt = now, requestCount = 1)
                active.requestCount < maxRequests -> active.copy(requestCount = active.requestCount + 1)
                else -> {
                    decision = McpRateLimitDecision(
                        allowed = false,
                        retryAfterSeconds = (windowSeconds - (now - active.startedAt)).coerceAtLeast(1)
                    )
                    active
                }
            }
        }
        return decision
    }

    private fun boundedCredentialKey(credentialKey: String, now: Long): String {
        if (windows.containsKey(credentialKey) || windows.size < maxTrackedCredentials) {
            return credentialKey
        }
        windows.entries.removeIf { now - it.value.startedAt >= windowSeconds }
        return if (windows.size < maxTrackedCredentials) credentialKey else OVERFLOW_CREDENTIAL_KEY
    }

    private companion object {
        const val OVERFLOW_CREDENTIAL_KEY = "overflow"
    }
}
