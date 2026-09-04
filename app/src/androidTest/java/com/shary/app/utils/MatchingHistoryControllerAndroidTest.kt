package com.shary.app.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shary.app.core.domain.types.valueobjects.MatchingState
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MatchingHistoryControllerAndroidTest {

    @Test
    fun undoAndRedoRunOnTheDeviceApi() {
        var current = state(0)
        val controller = MatchingHistoryController(
            snapshot = { current },
            restore = { current = it }
        )

        controller.commit()
        current = state(1)
        controller.undo()
        assertEquals(state(0), current)

        controller.redo()
        assertEquals(state(1), current)
    }

    private fun state(index: Int) = MatchingState(
        selectedStorageIndex = index,
        selectedRequestIndex = index,
        isStorageFirst = true,
        matches = emptyList(),
        freeLabelsFromUnmatched = emptyList()
    )
}
