package com.shary.app.core.domain.types.valueobjects

import org.junit.Assert.assertEquals
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
}
