package com.shary.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import com.shary.app.core.domain.types.enums.Tag

class SharySectionControlsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun toolbarSearchTakesFullWidthAndActionsStayOnSameRow() {
        composeTestRule.setContent {
            Box(Modifier.width(320.dp)) {
                SharySectionToolbar(
                    query = "",
                    onQueryChange = {}
                ) {
                    SharyIconButton(
                        icon = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        onClick = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Search").assertExists()
        val toolbarBounds = composeTestRule.onNodeWithTag("shary-section-toolbar")
            .getUnclippedBoundsInRoot()
        val searchBounds = composeTestRule.onNodeWithTag("shary-search-bar")
            .getUnclippedBoundsInRoot()
        val actionBounds = composeTestRule.onNodeWithContentDescription("More options")
            .getUnclippedBoundsInRoot()
        val toolbarWidth = toolbarBounds.right - toolbarBounds.left
        val searchWidth = searchBounds.right - searchBounds.left
        val ratio = searchWidth / toolbarWidth
        val searchCenterY = (searchBounds.top.value + searchBounds.bottom.value) / 2f
        val actionCenterY = (actionBounds.top.value + actionBounds.bottom.value) / 2f

        assertTrue("Search ratio was $ratio", ratio > 0.5f)
        assertTrue(
            kotlin.math.abs(searchCenterY - actionCenterY) <= 1f
        )
    }

    @Test
    fun longSoftButtonLabelDoesNotChangeFixedBounds() {
        composeTestRule.setContent {
            SharySoftButton(
                text = "very-long-shary-tag-should-be-hidden",
                onClick = {},
                modifier = Modifier
                    .width(132.dp)
                    .testTag("fixed-soft-button")
            )
        }

        val bounds = composeTestRule.onNodeWithTag("fixed-soft-button")
            .getUnclippedBoundsInRoot()
        assertEquals(132.dp, bounds.right - bounds.left)
        assertEquals(46.dp, bounds.bottom - bounds.top)
    }

    @Test
    fun fieldsToolbarKeepsSortStaticWhileTagsAreDirectAndScrollable() {
        composeTestRule.setContent {
            FieldsToolbar(
                query = "",
                onQueryChange = {},
                filters = listOf("All", "Identity", "Health", "Finance", "Contact"),
                activeFilter = "All",
                onFilterChange = {},
                sortAction = {
                    SharySoftButton(text = "Key", onClick = {})
                },
                overflowAction = {
                    SharyIconButton(
                        icon = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        onClick = {}
                    )
                }
            )
        }

        composeTestRule.onNodeWithTag("fields-static-sort").assertExists()
        composeTestRule.onNodeWithTag("fields-tag-scroller").assertExists()
        composeTestRule.onNodeWithText("All").assertExists()
        composeTestRule.onNodeWithText("Identity").assertExists()
    }

    @Test
    fun tagChipKeepsFixedBoundsForLongLabels() {
        composeTestRule.setContent {
            TagChip(
                tag = Tag.Entry("very-long-shary-tag-label", Color.Blue),
                modifier = Modifier.testTag("fixed-tag-chip")
            )
        }

        val bounds = composeTestRule.onNodeWithTag("fixed-tag-chip")
            .getUnclippedBoundsInRoot()
        assertEquals(82.dp, bounds.right - bounds.left)
        assertEquals(24.dp, bounds.bottom - bounds.top)
    }
}
