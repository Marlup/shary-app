package com.shary.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class SharyModalPolicyTest {

    @Test
    fun compactTaskUsesBottomSheetOnPhoneAndDialogAtMediumWidth() {
        assertEquals(
            SharyModalPresentation.BottomSheet,
            SharyModalPolicy.presentation(320, SharyModalComplexity.Compact)
        )
        assertEquals(
            SharyModalPresentation.Dialog,
            SharyModalPolicy.presentation(600, SharyModalComplexity.Compact)
        )
    }

    @Test
    fun complexTaskStaysFullScreenUntilExpandedWidth() {
        assertEquals(
            SharyModalPresentation.FullScreen,
            SharyModalPolicy.presentation(412, SharyModalComplexity.Complex)
        )
        assertEquals(
            SharyModalPresentation.FullScreen,
            SharyModalPolicy.presentation(839, SharyModalComplexity.Complex)
        )
        assertEquals(
            SharyModalPresentation.Dialog,
            SharyModalPolicy.presentation(840, SharyModalComplexity.Complex)
        )
    }

    @Test
    fun busyStateBlocksDismissAndDirtyStateRequiresConfirmation() {
        assertEquals(
            SharyModalDismissal.Block,
            SharyModalPolicy.dismissal(SharyModalOperationState.Saving, isDirty = true)
        )
        assertEquals(
            SharyModalDismissal.ConfirmDiscard,
            SharyModalPolicy.dismissal(SharyModalOperationState.Idle, isDirty = true)
        )
        assertEquals(
            SharyModalDismissal.Dismiss,
            SharyModalPolicy.dismissal(SharyModalOperationState.Saved, isDirty = false)
        )
    }
}
