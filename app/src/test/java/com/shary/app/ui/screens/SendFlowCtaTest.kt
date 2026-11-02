package com.shary.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SendFlowCtaTest {

    @Test
    fun fieldsWithoutUsersGuideToContactSelection() {
        val cta = fieldSendFlowCta(
            hasSelectedFields = true,
            hasSelectedUsers = false
        )

        assertEquals("Select Contacts", cta?.label)
        assertEquals(SendFlowDestination.CONTACTS, cta?.destination)
    }

    @Test
    fun fieldsWithUsersContinueToReview() {
        val cta = fieldSendFlowCta(
            hasSelectedFields = true,
            hasSelectedUsers = true
        )

        assertEquals("Review & send", cta?.label)
        assertEquals(SendFlowDestination.FIELD_REVIEW, cta?.destination)
    }

    @Test
    fun usersWithoutPayloadGuideToFieldSelection() {
        val cta = userSendFlowCta(
            hasSelectedUsers = true,
            hasSelectedFields = false,
            hasSelectedRequestFields = false
        )

        assertEquals("Select Fields", cta?.label)
        assertEquals(SendFlowDestination.FIELDS, cta?.destination)
    }

    @Test
    fun usersWithFieldsContinueToFieldReview() {
        val cta = userSendFlowCta(
            hasSelectedUsers = true,
            hasSelectedFields = true,
            hasSelectedRequestFields = false
        )

        assertEquals("Review & send", cta?.label)
        assertEquals(SendFlowDestination.FIELD_REVIEW, cta?.destination)
    }

    @Test
    fun usersWithRequestPayloadKeepRequestReviewRoute() {
        val cta = userSendFlowCta(
            hasSelectedUsers = true,
            hasSelectedFields = false,
            hasSelectedRequestFields = true
        )

        assertEquals("Review & send", cta?.label)
        assertEquals(SendFlowDestination.REQUEST_REVIEW, cta?.destination)
    }

    @Test
    fun outgoingRequestDraftWinsWhenBothPayloadCachesArePopulated() {
        val cta = userSendFlowCta(
            hasSelectedUsers = true,
            hasSelectedFields = true,
            hasSelectedRequestFields = true
        )

        assertEquals(SendFlowDestination.REQUEST_REVIEW, cta?.destination)
    }

    @Test
    fun noLocalSelectionHasNoCta() {
        assertNull(fieldSendFlowCta(hasSelectedFields = false, hasSelectedUsers = true))
        assertNull(
            userSendFlowCta(
                hasSelectedUsers = false,
                hasSelectedFields = true,
                hasSelectedRequestFields = true
            )
        )
    }
}
