package com.shary.app.ui.screens.summary

import com.shary.app.ui.screens.home.utils.SendOption
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewPrivacyCopyTest {

    @Test
    fun cloudCopyDisclosesDeviceEncryptionAndRetention() {
        val copy = reviewPrivacyCopy(
            method = SendOption.Cloud,
            payloadKind = ReviewPayloadKind.FIELD_VALUES,
            recipientCount = 2,
            sensitiveFieldCount = 1,
            cloudExpiryDays = 7
        )

        assertTrue(copy.disclosure.contains("separate payload"))
        assertTrue(copy.disclosure.contains("7-day expiry"))
        assertNull(copy.sensitiveWarning)
    }

    @Test
    fun emailFieldCopyDisclosesReadableValuesRecipientVisibilityAndUnknownDelivery() {
        val copy = reviewPrivacyCopy(
            method = SendOption.Email,
            payloadKind = ReviewPayloadKind.FIELD_VALUES,
            recipientCount = 3,
            sensitiveFieldCount = 2,
            cloudExpiryDays = 7
        )

        assertTrue(copy.disclosure.contains("readable JSON attachment"))
        assertTrue(copy.disclosure.contains("All 3 addresses"))
        assertTrue(copy.disclosure.contains("cannot confirm sending or delivery"))
        assertTrue(copy.disclosure.contains("scheduled for deletion after 24 hours"))
        assertTrue(copy.disclosure.contains("when Shary next starts"))
        assertTrue(copy.sensitiveWarning.orEmpty().contains("2 sensitive fields"))
        assertFalse(copy.disclosure.contains("never stored", ignoreCase = true))
    }

    @Test
    fun emailRequestCopyDoesNotClaimFieldValuesAreAttached() {
        val copy = reviewPrivacyCopy(
            method = SendOption.Email,
            payloadKind = ReviewPayloadKind.REQUESTED_KEYS,
            recipientCount = 1,
            sensitiveFieldCount = 0,
            cloudExpiryDays = 14
        )

        assertTrue(copy.disclosure.contains("field names only"))
        assertTrue(copy.disclosure.contains("not their values"))
        assertNull(copy.sensitiveWarning)
    }
}
