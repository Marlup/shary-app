package com.shary.app.ui.screens.field.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.types.enums.Tag
import com.shary.app.core.domain.types.valueobjects.FieldValueContract
import com.shary.app.core.domain.types.valueobjects.FieldValueSpec
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class FieldDetailDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun actionButtonsInvokeExpectedCallbacks() {
        var copyCount = 0
        var duplicateCount = 0
        var editValueCount = 0
        var editTagCount = 0
        var editAliasCount = 0
        var deleteCount = 0

        composeTestRule.setContent {
            FieldDetailDialog(
                keyName = "website",
                value = "https://example.com",
                alias = "main",
                tag = Tag.Unknown,
                typedValueLabel = "Link/URL",
                onCopyValue = { copyCount++ },
                onDuplicateField = { duplicateCount++ },
                onEditValue = { editValueCount++ },
                onEditTag = { editTagCount++ },
                onEditAlias = { editAliasCount++ },
                onDeleteField = { deleteCount++ },
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("Copy value").performClick()
        composeTestRule.onNodeWithText("Duplicate").performClick()
        composeTestRule.onNodeWithText("Edit value").performClick()
        composeTestRule.onNodeWithText("Edit tag").performClick()
        composeTestRule.onNodeWithText("Edit alias").performClick()
        composeTestRule.onNodeWithText("Delete").performClick()

        assertEquals(1, copyCount)
        assertEquals(1, duplicateCount)
        assertEquals(1, editValueCount)
        assertEquals(1, editTagCount)
        assertEquals(1, editAliasCount)
        assertEquals(1, deleteCount)
    }

    @Test
    fun listValuesAreRenderedAsVerticalDetailItems() {
        composeTestRule.setContent {
            FieldDetailDialog(
                keyName = "servers",
                value = "api,worker",
                valueItems = listOf("api", "worker"),
                alias = "runtime targets",
                tag = Tag.Unknown,
                typedValueLabel = "List",
                onCopyValue = {},
                onDuplicateField = {},
                onEditValue = {},
                onEditTag = {},
                onEditAlias = {},
                onDeleteField = {},
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("api").assertExists()
        composeTestRule.onNodeWithText("worker").assertExists()
    }

    @Test
    fun updateFieldDialogCanEditAndAddListElements() {
        var savedField: FieldDomain? = null
        val encodedListValue = FieldValueContract.encode(
            data = "api,worker",
            spec = FieldValueSpec(
                kind = "list",
                options = mapOf("separator" to ",")
            )
        )
        val targetField = FieldDomain(
            key = "servers",
            value = encodedListValue,
            keyAlias = "runtime targets",
            tag = Tag.Unknown,
            dateAdded = Instant.now()
        )

        composeTestRule.setContent {
            UpdateFieldDialog(
                targetField = targetField,
                onDismiss = {},
                onUpdateField = { savedField = it }
            )
        }

        composeTestRule.onNodeWithTag("field-list-element-0")
            .performTextReplacement("api-prod")
        composeTestRule.onNodeWithTag("field-list-pending-value")
            .performTextReplacement("scheduler")
        composeTestRule.onNodeWithText("+").performClick()
        composeTestRule.onNodeWithText("Save")
            .performScrollTo()
            .performClick()

        val parsed = FieldValueContract.parse(savedField!!.value)
        assertEquals("api-prod,worker,scheduler", parsed.plainData)
        assertEquals("list", parsed.envelope!!.spec!!.kind)
    }
}
