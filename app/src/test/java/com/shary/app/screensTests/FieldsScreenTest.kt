package com.shary.app.screensTests

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.rememberNavController
import com.shary.app.Field
import com.shary.app.core.Session
import com.shary.app.services.field.FieldService
import com.shary.app.ui.screens.fields.FieldsScreen
import com.shary.app.viewmodels.field.FieldViewModel
import com.shary.app.viewmodels.ViewModelFactory
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.*

class FieldsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var mockSession: Session
    private lateinit var mockFieldService: FieldService
    private lateinit var mockViewModel: FieldViewModel
    private lateinit var viewModelFactory: ViewModelFactory<FieldViewModel>

    private val fakeFields = listOf(
        Field.newBuilder().setKey("email").setKeyAlias("mail").setValue("test@abc.com").setDateAdded(0).build(),
        Field.newBuilder().setKey("phone").setKeyAlias("mobile").setValue("123456").setDateAdded(0).build()
    )

    @Before
    fun setup() {
        mockSession = mockk(relaxed = true)
        mockFieldService = mockk(relaxed = true)

        mockViewModel = mockk(relaxed = true)
        every { mockViewModel.fields } returns MutableStateFlow(fakeFields)
        every { mockViewModel.selectedKeys } returns MutableStateFlow(emptyList())

        viewModelFactory = ViewModelFactory { mockViewModel }
    }

    private fun launchScreen() {
        composeRule.setContent {
            FieldsScreen(
                navController = rememberNavController(),
                session = mockSession,
                fieldService = mockFieldService,
                fieldViewModelFactory = viewModelFactory
            )
        }
    }

    @Test
    fun rendersFieldScreenWithList() {
        launchScreen()
        composeRule.onNodeWithText("Fields").assertIsDisplayed()
        composeRule.onNodeWithText("email").assertIsDisplayed()
        composeRule.onNodeWithText("phone").assertIsDisplayed()
    }

    @Test
    fun showsAddDialogWhenFabClicked() {
        launchScreen()
        composeRule.onAllNodesWithContentDescription("Add Field")[0].performClick()
        composeRule.onNodeWithText("Add new field").assertIsDisplayed()
    }

    @Test
    fun showsWarningWhenFieldListIsEmpty() {
        every { mockViewModel.fields } returns MutableStateFlow(emptyList())
        launchScreen()
        composeRule.onNodeWithText("No fields available").assertIsDisplayed()
    }

    @Test
    fun editDialogAppears_whenFieldClicked() {
        every { mockFieldService.fieldToTriple(any()) } returns Triple("email", "mail", "test@abc.com")
        every { mockFieldService.valuesToField(any(), any(), any()) } returns fakeFields[0]

        launchScreen()
        composeRule.onNodeWithText("test@abc.com").performClick()
        composeRule.onNodeWithText("Update email").assertIsDisplayed()
    }

    @Test
    fun deleteButton_disabledWhenNoSelection() {
        launchScreen()
        composeRule.onAllNodesWithContentDescription("Delete Selected")[0]
            .performClick()
            .assertHasClickAction()
            .performClick()

        verify(exactly = 0) { mockViewModel.deleteField(any()) }
    }

    @Test
    fun deleteButton_callsViewModelWhenSelected() {
        every { mockViewModel.selectedKeys } returns MutableStateFlow(listOf("email"))
        launchScreen()
        composeRule.onAllNodesWithContentDescription("Delete Selected")[0].performClick()
        verify { mockViewModel.deleteField("email") }
        verify { mockViewModel.clearSelectedKeys() }
    }
}
