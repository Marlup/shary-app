package com.shary.app.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SharySectionNavigationBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun showsConnectTabAndDispatchesSelection() {
        var selected = SectionTab.FIELDS

        composeTestRule.setContent {
            SharySectionNavigationBar(
                currentTab = selected,
                onTabSelected = { selected = it }
            )
        }

        composeTestRule.onNodeWithText("Connect").assertExists()
        composeTestRule.onNodeWithText("Connect").performClick()

        assertEquals(SectionTab.CONNECT, selected)
    }
}
