package com.shary.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SharyIconButtonTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun disabledButtonUsesDisabledCallbackWhenProvided() {
        var clickCount = 0
        var disabledClickCount = 0

        composeTestRule.setContent {
            SharyIconButton(
                icon = Icons.Default.Search,
                contentDescription = "Search",
                enabled = false,
                onClick = { clickCount++ },
                onDisabledClick = { disabledClickCount++ }
            )
        }

        composeTestRule.onNodeWithContentDescription("Search").performClick()

        assertEquals(0, clickCount)
        assertEquals(1, disabledClickCount)
    }
}
