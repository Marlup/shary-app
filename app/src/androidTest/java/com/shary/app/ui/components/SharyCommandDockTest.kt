package com.shary.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SharyCommandDockTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun fullWidthDockUsesFixedIconButtonsForDeselectAndDelete() {
        var clearCount = 0

        composeTestRule.setContent {
            SharyCommandDock(
                selectedCount = 2,
                onClearSelection = { clearCount++ },
                primaryAction = DockAction("Select Users", icon = Icons.AutoMirrored.Filled.Send),
                secondaryActions = emptyList(),
                destructiveAction = DockAction(
                    label = "",
                    icon = Icons.Default.Delete,
                    contentDescription = "Delete selected items"
                ),
                onPrimaryClick = {},
                visualStyle = DockVisualStyle.FullWidth
            )
        }

        composeTestRule.onNodeWithContentDescription("Deselect all")
            .assertWidthIsEqualTo(52.dp)
            .assertHeightIsEqualTo(52.dp)
            .performClick()
        composeTestRule.onNodeWithContentDescription("Delete selected items")
            .assertWidthIsEqualTo(52.dp)
            .assertHeightIsEqualTo(52.dp)
        composeTestRule.onAllNodesWithText("2 selected").assertCountEquals(0)

        assertEquals(1, clearCount)
    }

    @Test
    fun iconOnlyDockKeepsFixedDeselectButton() {
        composeTestRule.setContent {
            SharyCommandDock(
                selectedCount = 1,
                onClearSelection = {},
                primaryAction = DockAction("Review", icon = Icons.AutoMirrored.Filled.Send),
                secondaryActions = emptyList(),
                destructiveAction = null,
                onPrimaryClick = {},
                visualStyle = DockVisualStyle.IconOnly
            )
        }

        composeTestRule.onNodeWithContentDescription("Deselect all")
            .assertWidthIsEqualTo(52.dp)
            .assertHeightIsEqualTo(52.dp)
    }
}
