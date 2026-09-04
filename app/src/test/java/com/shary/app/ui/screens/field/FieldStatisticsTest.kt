package com.shary.app.ui.screens.field

import androidx.compose.ui.graphics.Color
import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.types.enums.Tag
import org.junit.Assert.assertEquals
import org.junit.Test

class FieldStatisticsTest {

    @Test
    fun groupsCountsByTagAndSortsLargestFirst() {
        val work = Tag.Entry("Work", Color.Blue)
        val personal = Tag.Entry("Personal", Color.Red)

        val counts = buildFieldTagCounts(
            listOf(
                FieldDomain.create("email", "a@b.com", tag = work),
                FieldDomain.create("phone", "123", tag = work),
                FieldDomain.create("city", "Madrid", tag = personal),
                FieldDomain.create("misc", "value", tag = Tag.Unknown)
            )
        )

        assertEquals(
            listOf(
                FieldTagCount(tagName = "Work", count = 2),
                FieldTagCount(tagName = "Personal", count = 1),
                FieldTagCount(tagName = "Unknown", count = 1)
            ),
            counts
        )
    }
}
