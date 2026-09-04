package com.shary.app.ui.screens.field.components

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.shary.app.core.domain.types.enums.Tag
import com.shary.app.viewmodels.tag.TagEditorOperation
import com.shary.app.viewmodels.tag.TagEditorOperationState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class CompactTagSelectorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun compactSelectorExposesExistingCreateTagDialog() {
        val tag = Tag.Entry("Work", Color.Blue)
        composeTestRule.setContent {
            CompactTagSelector(
                selectedTag = tag,
                onTagSelected = {},
                availableTags = listOf(tag),
                onCreateTag = { _, _ -> }
            )
        }

        composeTestRule.onNodeWithText("Work").performClick()
        composeTestRule.onNodeWithText("Create new tag").performClick()
        composeTestRule.onNodeWithText("Create new tag").assertExists()
    }

    @Test
    fun injectedTagsWithoutCreationDependencyStayHiltFree() {
        val tag = Tag.Entry("Work", Color.Blue)
        composeTestRule.setContent {
            CompactTagSelector(
                selectedTag = tag,
                onTagSelected = {},
                availableTags = listOf(tag)
            )
        }

        composeTestRule.onNodeWithText("Work").performClick()
        composeTestRule.onNodeWithText("Create new tag").assertDoesNotExist()
    }

    @Test
    fun createWaitsForPersistedSuccessAndKeepsFailureDraftVisible() {
        val existing = Tag.Entry("Work", Color.Blue)
        val operationState = mutableStateOf<TagEditorOperationState>(TagEditorOperationState.Idle)
        var selected: Tag? = null
        var submitCount = 0

        composeTestRule.setContent {
            CompactTagSelector(
                selectedTag = existing,
                onTagSelected = { selected = it },
                availableTags = listOf(existing),
                onCreateTag = { name, _ ->
                    submitCount++
                    operationState.value =
                        TagEditorOperationState.Saving(TagEditorOperation.ADD, name.lowercase())
                },
                tagEditorState = operationState.value,
                onResetTagEditorState = {
                    operationState.value = TagEditorOperationState.Idle
                }
            )
        }

        composeTestRule.onNodeWithText("Work").performClick()
        composeTestRule.onNodeWithText("Create new tag").performClick()
        composeTestRule.onNodeWithText("Tag name").performTextInput("Personal")
        composeTestRule.onNodeWithText("Save").performClick()

        assertEquals(1, submitCount)
        assertNull(selected)
        composeTestRule.onNodeWithText("Saving...").assertIsNotEnabled()

        operationState.value = TagEditorOperationState.Failed(
            TagEditorOperation.ADD,
            "personal",
            "Disk write failed"
        )
        composeTestRule.onNodeWithText("Disk write failed").assertExists()
        composeTestRule.onNode(hasText("Tag name") and hasText("Personal")).assertExists()
        assertNull(selected)

        val persisted = Tag.Entry("Personal", Color.Green)
        operationState.value = TagEditorOperationState.Saved(
            TagEditorOperation.ADD,
            "personal",
            persisted
        )
        composeTestRule.waitForIdle()

        assertEquals(persisted, selected)
        composeTestRule.onNodeWithText("Create new tag").assertDoesNotExist()
    }
}
