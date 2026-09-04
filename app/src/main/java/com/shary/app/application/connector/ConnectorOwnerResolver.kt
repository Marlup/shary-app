package com.shary.app.application.connector

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.shary.app.core.domain.types.valueobjects.ConnectorOwnerBinding
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectorOwnerResolver @Inject constructor(
    @ApplicationContext context: Context,
    private val firebaseAuth: FirebaseAuth
) {
    private val preferences = context.getSharedPreferences("remote_mcp_identity", Context.MODE_PRIVATE)

    fun currentBinding(
        agentProfileId: String = "local-agent",
        grantId: String = "local-grant"
    ): ConnectorOwnerBinding {
        val owner = firebaseAuth.currentUser
        val ownerUid = owner?.uid.orEmpty()
        check(ownerUid.isNotBlank() && !owner?.email.isNullOrBlank() && owner?.isEmailVerified == true) {
            "A verified signed-in Shary owner is required."
        }
        return ConnectorOwnerBinding(
            ownerUid = ownerUid,
            vaultId = vaultId(),
            agentProfileId = agentProfileId,
            grantId = grantId
        )
    }

    fun currentOrNull(): ConnectorOwnerBinding? = runCatching { currentBinding() }.getOrNull()

    fun vaultId(): String {
        val existing = preferences.getString(KEY_VAULT_ID, null)
        if (!existing.isNullOrBlank()) return existing
        val generated = "vault_${UUID.randomUUID()}"
        preferences.edit().putString(KEY_VAULT_ID, generated).apply()
        return generated
    }

    fun deviceId(): String {
        val existing = preferences.getString(KEY_DEVICE_ID, null)
        if (!existing.isNullOrBlank()) return existing
        val generated = "device_${UUID.randomUUID()}"
        preferences.edit().putString(KEY_DEVICE_ID, generated).apply()
        return generated
    }

    private companion object {
        const val KEY_VAULT_ID = "vault_id"
        const val KEY_DEVICE_ID = "device_id"
    }
}
