package com.shary.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SendMethodCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun disabledCardInvokesDisabledClick() {
        var clickCount = 0
        var disabledCount = 0

        composeTestRule.setContent {
            SendMethodCard(
                icon = Icons.Default.Cloud,
                title = "Cloud",
                description = "Secure upload",
                isSelected = false,
                onClick = { clickCount++ },
                enabled = false,
                onDisabledClick = { disabledCount++ }
            )
        }

        composeTestRule.onNodeWithText("Cloud").performClick()

        assertEquals(0, clickCount)
        assertEquals(1, disabledCount)
    }

    @Test
    fun enabledCardInvokesClick() {
        var clickCount = 0
        var disabledCount = 0

        composeTestRule.setContent {
            SendMethodCard(
                icon = Icons.Default.Cloud,
                title = "Cloud",
                description = "Secure upload",
                isSelected = false,
                onClick = { clickCount++ },
                enabled = true,
                onDisabledClick = { disabledCount++ }
            )
        }

        composeTestRule.onNodeWithText("Cloud").performClick()

        assertEquals(1, clickCount)
        assertEquals(0, disabledCount)
    }
}
