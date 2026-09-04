package com.shary.app.core.domain.types.valueobjects

import org.junit.Assert.assertEquals
import org.junit.Test

class TagNameContractTest {

    @Test
    fun validNameIsTrimmedAndWhitespaceIsCollapsed() {
        assertEquals(
            TagNameValidation.Valid("Project Alpha"),
            TagNameContract.validate("  Project   Alpha  ")
        )
    }

    @Test
    fun blankNameIsRejected() {
        assertEquals(
            TagNameValidation.Invalid(TagNameInvalidReason.BLANK),
            TagNameContract.validate("   ")
        )
    }

    @Test
    fun overlongNameIsRejectedInsteadOfSilentlyTruncated() {
        assertEquals(
            TagNameValidation.Invalid(TagNameInvalidReason.TOO_LONG),
            TagNameContract.validate("a".repeat(TagNameContract.MAX_LENGTH + 1))
        )
    }

    @Test
    fun unsupportedPunctuationIsRejected() {
        assertEquals(
            TagNameValidation.Invalid(TagNameInvalidReason.UNSUPPORTED_CHARACTERS),
            TagNameContract.validate("work/private")
        )
    }
}
