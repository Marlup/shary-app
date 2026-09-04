package com.shary.app.utils

import com.shary.app.core.domain.types.valueobjects.MatchingState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchingHistoryControllerTest {

    @Test
    fun undoAndRedoRestoreSnapshotsInOrder() {
        var current = state(0)
        val controller = MatchingHistoryController(
            snapshot = { current },
            restore = { current = it }
        )

        controller.commit()
        current = state(1)
        controller.commit()
        current = state(2)

        controller.undo()
        assertEquals(state(1), current)
        assertTrue(controller.canUndo)
        assertTrue(controller.canRedo)

        controller.undo()
        assertEquals(state(0), current)
        assertFalse(controller.canUndo)
        assertTrue(controller.canRedo)

        controller.redo()
        assertEquals(state(1), current)

        controller.redo()
        assertEquals(state(2), current)
        assertTrue(controller.canUndo)
        assertFalse(controller.canRedo)
    }

    @Test
    fun committingAfterUndoDiscardsRedoHistory() {
        var current = state(0)
        val controller = MatchingHistoryController(
            snapshot = { current },
            restore = { current = it }
        )

        controller.commit()
        current = state(1)
        controller.undo()
        assertTrue(controller.canRedo)

        current = state(2)
        controller.commit()

        assertFalse(controller.canRedo)
        controller.redo()
        assertEquals(state(2), current)
    }

    @Test
    fun emptyHistoryAndClearAreSafeNoOps() {
        var current = state(0)
        val controller = MatchingHistoryController(
            snapshot = { current },
            restore = { current = it }
        )

        controller.undo()
        controller.redo()
        assertEquals(state(0), current)

        controller.commit()
        current = state(1)
        controller.undo()
        controller.clear()

        assertFalse(controller.canUndo)
        assertFalse(controller.canRedo)
        assertEquals(state(0), current)
    }

    private fun state(index: Int) = MatchingState(
        selectedStorageIndex = index,
        selectedRequestIndex = index,
        isStorageFirst = index % 2 == 0,
        matches = listOf(Triple(index, index, index)),
        freeLabelsFromUnmatched = listOf(index)
    )
}
