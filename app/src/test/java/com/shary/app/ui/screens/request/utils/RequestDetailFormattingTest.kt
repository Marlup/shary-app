package com.shary.app.ui.screens.request.utils

import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RequestDetailFormattingTest {

    private val now = Instant.parse("2026-07-23T12:00:00Z")

    @Test
    fun relativeTimestampUsesCompactElapsedUnits() {
        assertEquals("Just now", formatRequestRelativeTimestamp(now.minusSeconds(30), now))
        assertEquals("1 minute ago", formatRequestRelativeTimestamp(now.minusSeconds(60), now))
        assertEquals("5 minutes ago", formatRequestRelativeTimestamp(now.minusSeconds(300), now))
        assertEquals("1 hour ago", formatRequestRelativeTimestamp(now.minusSeconds(3600), now))
        assertEquals("2 days ago", formatRequestRelativeTimestamp(now.minusSeconds(172800), now))
    }

    @Test
    fun unknownTimestampIsNotPresentedAsRealHistory() {
        assertEquals("Unknown", formatRequestRelativeTimestamp(Instant.EPOCH, now))
        assertEquals(
            "Unknown",
            formatRequestAbsoluteTimestamp(
                timestamp = Instant.EPOCH,
                zoneId = ZoneId.of("UTC"),
                locale = Locale.US
            )
        )
    }

    @Test
    fun absoluteTimestampUsesTheRequestedZoneAndLocale() {
        val formatted = formatRequestAbsoluteTimestamp(
            timestamp = Instant.parse("2026-07-23T10:15:30Z"),
            zoneId = ZoneId.of("UTC"),
            locale = Locale.US
        )

        assertTrue(formatted.contains("2026"))
        assertTrue(formatted.contains("10:15"))
    }
}
