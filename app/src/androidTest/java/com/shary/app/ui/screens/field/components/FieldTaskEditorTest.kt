package com.shary.app.ui.screens.field.components

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.types.enums.Tag
import com.shary.app.core.domain.types.valueobjects.FieldValueContract
import com.shary.app.core.domain.types.valueobjects.FieldValueSpec
import com.shary.app.core.domain.types.valueobjects.TaskItem
import com.shary.app.core.domain.types.valueobjects.TaskValueContract
import com.shary.app.core.domain.types.valueobjects.TaskValueValidationIssue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class FieldTaskEditorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun blankTaskBlocksTypeChangeAndSaveUntilRepaired() {
        var savedField: FieldDomain? = null
        val taskValue = FieldValueContract.encode(
            data = TaskValueContract.encode(listOf(TaskItem("Ship release"))),
            spec = FieldValueSpec(kind = "tasks")
        )
        val targetField = FieldDomain(
            key = "release",
            value = taskValue,
            keyAlias = "",
            tag = Tag.Unknown,
            dateAdded = Instant.now()
        )

        composeTestRule.setContent {
            UpdateFieldDialog(
                targetField = targetField,
                onDismiss = {},
                onUpdateField = { savedField = it },
                availableTags = listOf(Tag.Unknown)
            )
        }

        composeTestRule.onNodeWithTag("field-task-title-0")
            .performTextReplacement("")
        composeTestRule.onNodeWithTag("field-value-type-selector")
            .performScrollTo()
            .performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag("field-value-type-option-text")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithTag("field-value-type-option-text").performClick()

        composeTestRule.onNodeWithTag("field-task-title-0").assertExists()
        composeTestRule.onNodeWithText(TaskValueValidationIssue.BLANK_TITLE.message)
            .assertExists()
        composeTestRule.onNodeWithText("Save")
            .performScrollTo()
            .assertIsNotEnabled()
        assertNull(savedField)

        composeTestRule.onNodeWithTag("field-task-title-0")
            .performTextReplacement("Publish release")
        composeTestRule.onNodeWithText("Save")
            .performScrollTo()
            .assertIsEnabled()
            .performClick()

        assertNotNull(savedField)
        val parsed = FieldValueContract.parse(savedField!!.value)
        val tasks = TaskValueContract.decode(parsed.plainData).getOrThrow()
        assertEquals(listOf(TaskItem("Publish release")), tasks)
    }
}
