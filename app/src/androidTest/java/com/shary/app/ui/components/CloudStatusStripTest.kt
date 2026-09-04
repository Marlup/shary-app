package com.shary.app.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.shary.app.ui.utils.CloudUiStatus
import org.junit.Rule
import org.junit.Test

class CloudStatusStripTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun readyStatusShowsCompactPendingCopy() {
        composeTestRule.setContent {
            CloudStatusStrip(
                status = CloudUiStatus(
                    ready = true,
                    reason = "",
                    label = "Ready",
                    detail = "Cloud inbox and secure relay are available.",
                    safetyLabel = "cloud-ready"
                ),
                pendingCount = 3
            )
        }

        composeTestRule.onNodeWithText("Cloud ready").assertExists()
        composeTestRule.onNodeWithText("3 pending").assertExists()
    }
}
