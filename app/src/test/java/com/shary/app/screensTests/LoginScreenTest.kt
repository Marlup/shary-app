package com.shary.app.screensTests

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.shary.app.core.session.Session
import com.shary.app.infrastructure.services.cloud.CloudServiceImpl
import com.shary.app.ui.screens.login.LoginScreen
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/*
----- Important -----
- relaxed = true permite que MockK genere respuestas por defecto para métodos no stubbeados, útil en Compose.

- every { ... } returns ... define un comportamiento concreto que luego verify { ... } puede comprobar.

- Para verificar navegación, podrías usar un TestNavHostController si lo deseas (puedo ayudarte con eso).
 */

class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @MockK
    private lateinit var mockContext: Context
    @MockK
    private lateinit var mockSession: Session
    @MockK
    private lateinit var mockCloudServiceImpl: CloudServiceImpl
    @MockK
    private lateinit var navController: NavHostController

    @Before
    fun setUp() {
        mockContext = mockk(relaxed = true)
        mockSession = mockk(relaxed = true)
        mockCloudServiceImpl = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun setupLoginScreen() {
        composeTestRule.setContent {
            navController = rememberNavController()
            LoginScreen(
                navController = navController,
                session = mockSession,
                cloudService = mockCloudServiceImpl
            )
        }
    }

    @Test
    fun loginScreen_displaysAllFieldsAndButtons() {
        setupLoginScreen()

        composeTestRule.onNodeWithText("Username").assertIsDisplayed()
        composeTestRule.onNodeWithText("Password").assertIsDisplayed()
        composeTestRule.onNodeWithText("Login").assertIsDisplayed()
        composeTestRule.onNodeWithText("Biometric Login").assertIsDisplayed()
    }

    @Test
    fun loginScreen_validCredentials_triggersSessionMethods() {
        every { mockSession.login(any(), "user", "pass") } returns true
        every { mockSession.sessionEmail } returns "user@example.com"

        setupLoginScreen()

        composeTestRule.onNodeWithText("Username").performTextInput("user")
        composeTestRule.onNodeWithText("Password").performTextInput("pass")
        composeTestRule.onNodeWithText("Login").performClick()

        verify { mockSession.logup(mockContext, "user", "email", "pass") }
        verify { mockSession.login(mockContext, "user", "pass") }
    }
}
