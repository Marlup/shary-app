package com.shary.app.utils

import androidx.compose.runtime.mutableStateListOf
import com.shary.app.core.domain.types.valueobjects.MatchingState

/**
 * Undo/Redo controller for field matching dialog.
 *
 * Stores full snapshots (selections + matches + free labels + ordering flag).
 * - commit(): push current snapshot to undo stack, clears redo stack
 * - undo(): restore previous snapshot
 * - redo(): restore next snapshot
 */
class MatchingHistoryController(
    private val snapshot: () -> MatchingState,
    private val restore: (MatchingState) -> Unit
) {
    private val past = mutableStateListOf<MatchingState>()
    private val future = mutableStateListOf<MatchingState>()

    val canUndo: Boolean get() = past.isNotEmpty()
    val canRedo: Boolean get() = future.isNotEmpty()

    fun clear() {
        past.clear()
        future.clear()
    }

    fun commit() {
        past.add(snapshot())
        future.clear()
    }

    fun undo() {
        if (!canUndo) return
        future.add(snapshot())
        val prev = past.removeLast()
        restore(prev)
    }

    fun redo() {
        if (!canRedo) return
        past.add(snapshot())
        val next = future.removeLast()
        restore(next)
    }
}