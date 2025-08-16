package com.shary.app.screensTests

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.rememberNavController
import com.shary.app.User
import com.shary.app.core.session.Session
import com.shary.app.services.user.UserService
import com.shary.app.ui.screens.user.UsersScreen
import com.shary.app.viewmodels.ViewModelFactory
import com.shary.app.viewmodels.user.UserViewModel
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.*

class UsersScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var session: Session
    private lateinit var userService: UserService
    private lateinit var viewModel: UserViewModel
    private lateinit var viewModelFactory: ViewModelFactory<UserViewModel>

    private val user1 = User.newBuilder().setUsername("Alice").setEmail("alice@example.com").setDateAdded(0L).build()
    private val user2 = User.newBuilder().setUsername("Bob").setEmail("bob@example.com").setDateAdded(0L).build()

    @Before
    fun setUp() {
        session = mockk(relaxed = true)
        userService = mockk(relaxed = true)

        viewModel = mockk(relaxed = true)
        every { viewModel.users } returns MutableStateFlow(listOf(user1, user2))
        every { viewModel.selectedEmails } returns MutableStateFlow(emptyList())
        every { viewModel.selectedPhoneNumber } returns MutableStateFlow("")

        viewModelFactory = ViewModelFactory { viewModel }
    }

    private fun launchScreen() {
        composeRule.setContent {
            UsersScreen(
                navController = rememberNavController(),
                session = session,
                userViewModelFactory = viewModelFactory,
                userService = userService
            )
        }
    }

    @Test
    fun usersAreDisplayedCorrectly() {
        launchScreen()
        composeRule.onNodeWithText("Users").assertIsDisplayed()
        composeRule.onNodeWithText("alice@example.com").assertIsDisplayed()
        composeRule.onNodeWithText("bob@example.com").assertIsDisplayed()
    }

    @Test
    fun addUserDialogAppears_onFabClick() {
        launchScreen()
        composeRule.onAllNodesWithContentDescription("Add Field")[0].performClick()
        composeRule.onNodeWithText("Add new user").assertIsDisplayed()
    }

    @Test
    fun showsNoUsersAvailable_whenListIsEmpty() {
        every { viewModel.users } returns MutableStateFlow(emptyList())
        launchScreen()
        composeRule.onNodeWithText("No users available").assertIsDisplayed()
    }

    @Test
    fun deleteButtonDoesNotTrigger_whenNoSelection() {
        launchScreen()
        composeRule.onAllNodesWithContentDescription("Delete users")[0].performClick()
        verify(exactly = 0) { viewModel.deleteUser(any()) }
    }

    @Test
    fun deleteButtonCallsViewModel_whenUserSelected() {
        every { viewModel.selectedEmails } returns MutableStateFlow(listOf("alice@example.com"))
        launchScreen()
        composeRule.onAllNodesWithContentDescription("Delete users")[0].performClick()
        verify { viewModel.deleteUser("alice@example.com") }
        verify { viewModel.clearSelectedUsers() }
    }

    @Test
    fun userFilteringByEmail_worksCorrectly() {
        every { viewModel.users } returns MutableStateFlow(listOf(user1, user2))
        launchScreen()
        composeRule.onNodeWithText("bob@example.com").assertExists()
        composeRule.onNodeWithText("Alice").assertExists()
        composeRule.onNodeWithText("Bob").assertExists()
    }
}
