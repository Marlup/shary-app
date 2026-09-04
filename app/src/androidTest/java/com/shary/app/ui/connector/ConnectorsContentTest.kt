package com.shary.app.ui.connector

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.shary.app.core.domain.types.enums.ConnectorClientStatus
import com.shary.app.core.domain.types.valueobjects.ConnectorClient
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ConnectorsContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun disabledContentShowsGuidanceAndToggleCallback() {
        var toggleValue: Boolean? = null

        composeTestRule.setContent {
            ConnectorsContent(
                mcpEnabled = false,
                connectorMessage = null,
                pendingApprovalCount = 2,
                renewalRequiredCount = 1,
                activeRemoteConnectorClients = listOf(sampleClient()),
                recentActivityLabels = listOf("shary.fields.list • approved"),
                onToggleMcp = { toggleValue = it },
                onRevokeConnector = {},
                onRevokeAll = {}
            )
        }

        composeTestRule.onNodeWithText("Enable MCP capability").assertExists()
        composeTestRule.onNodeWithText(
            "MCP is currently disabled. Approval links, background sync, and grant execution stay inactive until you enable this capability."
        ).assertExists()
        composeTestRule.onNodeWithContentDescription("Enable MCP capability switch").performClick()

        assertEquals(true, toggleValue)
    }

    @Test
    fun showsGrantAndActivityDetails() {
        composeTestRule.setContent {
            ConnectorsContent(
                mcpEnabled = true,
                connectorMessage = "MCP capability enabled",
                pendingApprovalCount = 0,
                renewalRequiredCount = 0,
                activeRemoteConnectorClients = listOf(sampleClient()),
                recentActivityLabels = listOf("shary.fields.list • approved"),
                onToggleMcp = {},
                onRevokeConnector = {},
                onRevokeAll = {}
            )
        }

        composeTestRule.onNodeWithText("Remote Codex").assertExists()
        composeTestRule.onNodeWithText("MCP capability enabled").assertExists()
        composeTestRule.onNodeWithText("- shary.fields.list • approved").assertExists()
    }

    private fun sampleClient() = ConnectorClient(
        clientId = "client_1",
        displayName = "Remote Codex",
        createdAt = 1_700_000_000,
        status = ConnectorClientStatus.PAIRED_FULL,
        lastActivityAt = 1_700_000_100,
        grantId = "grant_12345678"
    )
}
