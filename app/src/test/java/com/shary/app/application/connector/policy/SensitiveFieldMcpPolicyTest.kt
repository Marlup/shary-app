package com.shary.app.application.connector.policy

import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.types.valueobjects.FieldValueTypes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SensitiveFieldMcpPolicyTest {
    private val sensitiveValue =
        FieldValueTypes.encodeForStorage("secret", "sensitive").getOrThrow()

    @Test
    fun sensitive_read_and_update_are_blocked_by_default() {
        val field = FieldDomain.create("token", sensitiveValue)

        val read = runCatching { SensitiveFieldMcpPolicy.requireReadable(field) }
        val update = runCatching { SensitiveFieldMcpPolicy.requireUpdatable(field, null) }

        assertEquals(SensitiveFieldMcpPolicy.BLOCKED_REASON, read.exceptionOrNull()?.message)
        assertEquals(SensitiveFieldMcpPolicy.BLOCKED_REASON, update.exceptionOrNull()?.message)
    }

    @Test
    fun locally_shared_sensitive_field_can_be_read_and_updated_without_reclassification() {
        val field = FieldDomain.create("token", sensitiveValue).copy(isMcpShareable = true)

        assertTrue(runCatching { SensitiveFieldMcpPolicy.requireReadable(field) }.isSuccess)
        assertTrue(runCatching { SensitiveFieldMcpPolicy.requireUpdatable(field, null) }.isSuccess)
        assertTrue(
            runCatching {
                SensitiveFieldMcpPolicy.requireUpdatable(field, "sensitive")
            }.isSuccess
        )
    }

    @Test
    fun mcp_cannot_create_or_transition_sensitive_types() {
        val plain = FieldDomain.create("name", "value")

        assertEquals(
            SensitiveFieldMcpPolicy.CREATE_REASON,
            runCatching {
                SensitiveFieldMcpPolicy.requireCreatable("sensitive")
            }.exceptionOrNull()?.message
        )
        assertEquals(
            SensitiveFieldMcpPolicy.TYPE_CHANGE_REASON,
            runCatching {
                SensitiveFieldMcpPolicy.requireUpdatable(plain, "sensitive")
            }.exceptionOrNull()?.message
        )
    }
}
