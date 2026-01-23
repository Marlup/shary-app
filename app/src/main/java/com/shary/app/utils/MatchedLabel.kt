package com.shary.app.utils

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
private fun MatchedLabel(matchId: Int) {
    Text(
        text = "matched $matchId",
        style = MaterialTheme.typography.labelSmall
    )
}
