package com.shary.app.core.domain.types.valueobjects

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FieldValueContractTest {

    @Test
    fun validate_accepts_linkUrl_typed_kind_in_strict_mode() {
        val encoded = FieldValueContract.encode(
            data = "https://example.com",
            spec = FieldValueSpec(kind = "link_url")
        )

        val result = FieldValueContract.validate(encoded, strictModeEnabled = true)

        assertTrue(result.isValid)
    }

    @Test
    fun parse_returns_typed_and_preserves_link_kind() {
        val encoded = FieldValueContract.encode(
            data = "https://example.com",
            spec = FieldValueSpec(kind = "link_url")
        )

        val parsed = FieldValueContract.parse(encoded)

        assertEquals(FieldValueMode.TYPED, parsed.mode)
        assertEquals("link_url", parsed.envelope?.spec?.kind)
        assertEquals("https://example.com", parsed.plainData)
    }

    @Test
    fun email_type_is_listed_encoded_and_validated() {
        assertTrue(FieldValueTypes.available.any { it.name == "email" })

        val encoded = FieldValueTypes.encodeForStorage("person@example.com", "email").getOrThrow()
        val parsed = FieldValueContract.parse(encoded)

        assertEquals("email", parsed.envelope?.spec?.kind)
        assertEquals("person@example.com", parsed.plainData)
        assertTrue(FieldValueContract.validate(encoded, strictModeEnabled = true).isValid)
    }

    @Test
    fun email_type_rejects_invalid_value_and_plain_remains_unwrapped() {
        assertTrue(FieldValueTypes.encodeForStorage("not-an-email", "email").isFailure)
        assertEquals("plain value", FieldValueTypes.encodeForStorage("plain value", "plain").getOrThrow())
    }

    @Test
    fun tasks_type_round_trips_titles_and_statuses() {
        val taskJson = TaskValueContract.encode(
            listOf(
                TaskItem("Draft release notes"),
                TaskItem("Approve build", TaskStatus.REVIEW),
                TaskItem("Publish", TaskStatus.DONE)
            )
        )

        val encoded = FieldValueTypes.encodeForStorage(taskJson, "tasks").getOrThrow()
        val parsed = FieldValueContract.parse(encoded)
        val tasks = TaskValueContract.decode(parsed.plainData).getOrThrow()

        assertEquals("tasks", parsed.envelope?.spec?.kind)
        assertEquals(TaskStatus.TODO, tasks[0].status)
        assertEquals(TaskStatus.REVIEW, tasks[1].status)
        assertEquals(TaskStatus.DONE, tasks[2].status)
    }

    @Test
    fun tasks_reject_empty_or_blank_titles() {
        assertTrue(runCatching { TaskValueContract.encode(emptyList()) }.isFailure)
        assertTrue(
            runCatching {
                TaskValueContract.encode(listOf(TaskItem("   ")))
            }.isFailure
        )
    }

    @Test
    fun task_tryEncode_returns_typed_validation_without_throwing() {
        val empty = TaskValueContract.tryEncode(emptyList())
        assertEquals(
            TaskValueEncodingResult.Invalid(TaskValueValidationIssue.EMPTY),
            empty
        )

        val blank = TaskValueContract.tryEncode(
            listOf(
                TaskItem("Keep", TaskStatus.DOING),
                TaskItem("   ", TaskStatus.REVIEW),
                TaskItem("", TaskStatus.DONE)
            )
        )
        assertEquals(
            TaskValueEncodingResult.Invalid(
                issue = TaskValueValidationIssue.BLANK_TITLE,
                invalidIndices = listOf(1, 2)
            ),
            blank
        )
    }

    @Test
    fun task_tryEncode_trims_titles_and_preserves_status() {
        val result = TaskValueContract.tryEncode(
            listOf(TaskItem("  Ship release  ", TaskStatus.REVIEW))
        )

        assertTrue(result is TaskValueEncodingResult.Success)
        val decoded = TaskValueContract.decode(
            (result as TaskValueEncodingResult.Success).encodedValue
        ).getOrThrow()
        assertEquals(listOf(TaskItem("Ship release", TaskStatus.REVIEW)), decoded)
    }

    @Test
    fun sensitive_type_uses_exact_five_character_mask_until_revealed() {
        val encoded = FieldValueTypes.encodeForStorage("top secret", "sensitive").getOrThrow()

        assertEquals("*****", SENSITIVE_MASK)
        assertEquals(5, SENSITIVE_MASK.length)
        assertTrue(FieldValueTypes.isSensitive(encoded))
        assertEquals(SENSITIVE_MASK, FieldValueTypes.displayData(encoded))
        assertEquals("top secret", FieldValueTypes.displayData(encoded, revealSensitive = true))
        assertFalse(FieldValueTypes.isSensitive("plain"))
    }
}
