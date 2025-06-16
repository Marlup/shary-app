package com.shary.app.screensTests

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.navigation.NavHostController
import com.shary.app.core.session.Session
import com.shary.app.services.cloud.CloudService
import com.shary.app.ui.screens.logup.LogupScreen
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalMaterial3Api::class)
class LogupScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockNavController = mockk<NavHostController>(relaxed = true)
    private val mockSession = mockk<Session>(relaxed = true)
    private val mockCloudService = mockk<CloudService>(relaxed = true)

    @Test
    fun screen_shows_all_fields_and_button() {
        composeTestRule.setContent {
            LogupScreen(
                navController = mockNavController,
                session = mockSession,
                cloudService = mockCloudService
            )
        }

        // Check if all input fields and button exist
        composeTestRule.onNodeWithText("Email").assertExists()
        composeTestRule.onNodeWithText("Username").assertExists()
        composeTestRule.onNodeWithText("Create Account").assertExists()
    }

    @Test
    fun empty_fields_shows_validation_error() {
        composeTestRule.setContent {
            LogupScreen(
                navController = mockNavController,
                session = mockSession,
                cloudService = mockCloudService
            )
        }

        composeTestRule.onNodeWithText("Create Account").performClick()

        composeTestRule.waitUntil(timeoutMillis = 2000) {
            composeTestRule
                .onAllNodesWithText("Invalid email")
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun valid_fields_calls_session_and_navigates() {
        coEvery { mockCloudService.uploadUser(any()) } returns Pair(true, "token123")

        composeTestRule.setContent {
            LogupScreen(
                navController = mockNavController,
                session = mockSession,
                cloudService = mockCloudService
            )
        }

        composeTestRule.onNodeWithText("Email").performTextInput("test@email.com")
        composeTestRule.onNodeWithText("Username").performTextInput("testuser")
        composeTestRule.onAllNodes(hasTestTag("PasswordField"))[0].performTextInput("Test1234$")
        composeTestRule.onAllNodes(hasTestTag("PasswordField"))[1].performTextInput("Test1234$")

        composeTestRule.onNodeWithText("Create Account").performClick()

        coVerify { mockSession.cacheCredentials("test@email.com", "testuser", "Test1234$") }
        //coVerify { mockSession.generateKeys("Test1234$", "testuser") }
        coVerify { mockCloudService.uploadUser("test@email.com") }
        coVerify { mockSession.storeCachedCredentials(any()) }
        verify { mockNavController.navigate("login") }
    }


}
