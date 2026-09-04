package com.shary.app.application.request

import com.shary.app.core.domain.models.RequestedFieldDraft
import org.junit.Assert.assertEquals
import org.junit.Test

class RequestDraftEditorTest {

    @Test
    fun validCandidateIsNormalizedAndAdded() {
        val transition = addRequestedFieldToDraft(
            current = listOf(RequestedFieldDraft("address")),
            candidate = RequestedFieldDraft("  phone-number  ", "  Mobile  ")
        )

        assertEquals(RequestDraftFieldAddOutcome.ADDED, transition.outcome)
        assertEquals(
            listOf(
                RequestedFieldDraft("address"),
                RequestedFieldDraft("phone-number", "Mobile")
            ),
            transition.requestedFields
        )
    }

    @Test
    fun duplicateCandidateIsRejectedCaseInsensitivelyWithoutChangingDraft() {
        val current = listOf(RequestedFieldDraft("phone-number", "Mobile"))

        val transition = addRequestedFieldToDraft(
            current = current,
            candidate = RequestedFieldDraft(" PHONE-NUMBER ", "Replacement")
        )

        assertEquals(RequestDraftFieldAddOutcome.DUPLICATE, transition.outcome)
        assertEquals(current, transition.requestedFields)
    }

    @Test
    fun blankCandidateIsInvalidWithoutChangingDraft() {
        val current = listOf(RequestedFieldDraft("address"))

        val transition = addRequestedFieldToDraft(
            current = current,
            candidate = RequestedFieldDraft("   ", "Alias")
        )

        assertEquals(RequestDraftFieldAddOutcome.INVALID, transition.outcome)
        assertEquals(current, transition.requestedFields)
    }
}
