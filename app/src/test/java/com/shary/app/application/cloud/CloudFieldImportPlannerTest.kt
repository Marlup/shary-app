package com.shary.app.application.cloud

import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.types.enums.Tag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class CloudFieldImportPlannerTest {

    @Test
    fun `plan classifies new unchanged changed and duplicate keys`() {
        val existing = listOf(
            field("email", "old@example.com", "primary"),
            field("phone", "555", "")
        )
        val json = """
            {
              "email": {"value": "new@example.com", "alias": "primary"},
              "phone": {"value": "555", "alias": ""},
              "passport": "AA123",
              "passport": "AA123"
            }
        """.trimIndent()

        val plan = CloudFieldImportPlanner.plan(listOf(json), existing)

        assertEquals(listOf("passport"), plan.diff.addedKeys)
        assertEquals(listOf("email", "phone"), plan.diff.duplicateKeys)
        assertEquals(listOf("email"), plan.diff.changedKeys)
        assertEquals(listOf("phone"), plan.diff.unchangedKeys)
        assertEquals(listOf("passport"), plan.fieldsToCreate.map { it.key })
        assertTrue(plan.diff.requiresDetailedReview)
    }

    @Test
    fun `clean import does not require detailed review`() {
        val plan = CloudFieldImportPlanner.plan(
            jsonStrings = listOf("""{"passport":"AA123"}"""),
            existingFields = emptyList()
        )

        assertEquals(listOf("passport"), plan.diff.addedKeys)
        assertFalse(plan.diff.requiresDetailedReview)
        assertTrue(plan.diff.hasIncomingFields)
    }

    @Test
    fun `preview keys are sorted distinct and capped`() {
        val json = """
            {
              "zeta": "1",
              "alpha": "2",
              "Alpha": "duplicate case"
            }
        """.trimIndent()

        assertEquals(listOf("alpha", "zeta"), CloudFieldImportPlanner.previewKeys(json))
    }

    private fun field(key: String, value: String, alias: String): FieldDomain =
        FieldDomain(
            key = key,
            value = value,
            keyAlias = alias,
            tag = Tag.Unknown,
            dateAdded = Instant.EPOCH
        )
}
