package com.shary.app.core.domain.types.valueobjects

import kotlinx.serialization.Serializable

/**
 * Single state object that contains everything needed to restore the dialog
 */

@Serializable
data class MatchingState(
    val selectedStorageIndex: Int?,
    val selectedRequestIndex: Int?,
    val isStorageFirst: Boolean,
    val matches: List<Triple<Int, Int, Int>>,
    val freeLabelsFromUnmatched: List<Int>,
)

