package com.shary.app.screensTests

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.rememberNavController
import com.shary.app.core.session.Session
import com.shary.app.Field
import com.shary.app.services.bluetooth.BluetoothService
import com.shary.app.services.cloud.CloudService
import com.shary.app.services.email.EmailService
import com.shary.app.services.messaging.TelegramService
import com.shary.app.services.messaging.WhatsAppService
import com.shary.app.ui.screens.home.HomeScreen
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var session: Session
    private lateinit var emailService: EmailService
    private lateinit var cloudService: CloudService
    private lateinit var whatsappService: WhatsAppService
    private lateinit var telegramService: TelegramService
    private lateinit var bluetoothService: BluetoothService

    @Before
    fun setUp() {
        session = mockk(relaxed = true)
        every { session.selectedEmails } returns MutableStateFlow(emptyList())
        every { session.selectedFields } returns MutableStateFlow(emptyList())
        every { session.selectedPhoneNumber } returns MutableStateFlow("+123456789")

        emailService = mockk(relaxed = true)
        cloudService = mockk(relaxed = true)
        whatsappService = mockk(relaxed = true)
        telegramService = mockk(relaxed = true)
        bluetoothService = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun launchScreen() {
        composeTestRule.setContent {
            HomeScreen(
                navController = rememberNavController(),
                session = session,
                emailService = emailService,
                cloudService = cloudService,
                whatsAppService = whatsappService,
                telegramService = telegramService,
                bluetoothService = bluetoothService
            )
        }
    }

    @Test
    fun allNavigationButtons_areVisible() {
        launchScreen()

        composeTestRule.onNodeWithText("Fields").assertIsDisplayed()
        composeTestRule.onNodeWithText("Users").assertIsDisplayed()
        composeTestRule.onNodeWithText("Requests").assertIsDisplayed()
        composeTestRule.onNodeWithText("File Visualizer").assertIsDisplayed()
    }

    @Test
    fun emptyState_showsWarningMessage() {
        launchScreen()
        composeTestRule.onNodeWithText("Select at least one field and one user to send the data").assertIsDisplayed()
    }

    @Test
    fun onlyEmailsSelected_showsWarningMessage() {
        every { session.selectedEmails } returns MutableStateFlow(listOf("test@example.com"))
        every { session.selectedFields } returns MutableStateFlow(emptyList())

        launchScreen()
        composeTestRule.onNodeWithText("Users selected. Select at least one field to send the data").assertIsDisplayed()
    }

    @Test
    fun onlyFieldsSelected_showsWarningMessage() {
        every { session.selectedEmails } returns MutableStateFlow(emptyList())
        every { session.selectedFields } returns MutableStateFlow(listOf(Field
            .newBuilder()
            .setKey("key")
            .setValue("value")
            .setKeyAlias("alias")
            .setDateAdded(0)
            .build()
        ))

        launchScreen()
        composeTestRule.onNodeWithText("Fields selected. Select at least one user to send the data").assertIsDisplayed()
    }

    // 🔜 Test de diálogos interactivos → opcional si implementas ComposeTestRule.robot
}
