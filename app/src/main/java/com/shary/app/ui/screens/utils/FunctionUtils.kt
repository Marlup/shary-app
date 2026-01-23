package com.shary.app.ui.screens.utils

import androidx.compose.runtime.snapshots.SnapshotStateList

object FunctionUtils {

    fun matchIfPossible(
        storageIdx: Int?,
        requestIdx: Int?,
        matches: SnapshotStateList<Triple<Int, Int, Int>>,
        freeLabelsFromUnmatched: SnapshotStateList<Int>,
        isStorageFirst: Boolean,
        onUnselect: () -> Unit,
        onIsStorageFirst: (Boolean) -> Unit,
        onFreeLabelStored: (Int) -> Unit
    ) {
        if (storageIdx != null && requestIdx == null) onIsStorageFirst(true)
        if (storageIdx == null && requestIdx != null) onIsStorageFirst(false)

        if (storageIdx == null || requestIdx == null) return

        val storageMatchIndex = matches.indexOfFirst { it.first == storageIdx }
        val requestMatchIndex = matches.indexOfFirst { it.second == requestIdx }

        if ((storageMatchIndex == requestMatchIndex) && (storageMatchIndex != -1)) {
            onUnselect()
            return
        }

        val isStorageMatched = storageMatchIndex != -1
        val isRequestMatched = requestMatchIndex != -1

        when {
            !isStorageMatched && !isRequestMatched -> {
                val newLabel = if (freeLabelsFromUnmatched.isEmpty()) {
                    matches.count() + 1
                } else {
                    val reused = freeLabelsFromUnmatched.first()
                    freeLabelsFromUnmatched.removeAt(0)
                    reused
                }
                val newMatch = Triple(storageIdx, requestIdx, newLabel)
                matches.add(newMatch)
            }

            isStorageMatched && !isRequestMatched -> {
                val oldMatch = matches[storageMatchIndex]
                matches[storageMatchIndex] = Triple(oldMatch.first, requestIdx, oldMatch.third)
            }

            !isStorageMatched && isRequestMatched -> {
                val oldMatch = matches[requestMatchIndex]
                matches[requestMatchIndex] = Triple(storageIdx, oldMatch.second, oldMatch.third)
            }

            else -> {
                val storageOldMatch = matches[storageMatchIndex].copy()
                val requestOldMatch = matches[requestMatchIndex].copy()

                if (storageMatchIndex > requestMatchIndex) {
                    matches.removeAt(storageMatchIndex)
                    matches.removeAt(requestMatchIndex)
                } else {
                    matches.removeAt(requestMatchIndex)
                    matches.removeAt(storageMatchIndex)
                }

                val newTriple = if (isStorageFirst) {
                    matches.add(Triple(storageIdx, requestIdx, storageOldMatch.third))
                    onFreeLabelStored(requestOldMatch.third)
                    Triple(storageIdx, requestIdx, storageOldMatch.third)
                } else {
                    matches.add(Triple(storageIdx, requestIdx, requestOldMatch.third))
                    onFreeLabelStored(storageOldMatch.third)
                    Triple(storageIdx, requestIdx, requestOldMatch.third)
                }
            }
        }

        onUnselect()
    }
}
