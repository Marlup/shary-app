package com.shary.app.core.domain.types.enums

import androidx.compose.ui.graphics.Color
import com.shary.app.core.domain.types.enums.UiFieldTag.Bank
import com.shary.app.core.domain.types.enums.UiFieldTag.Custom
import com.shary.app.core.domain.types.enums.UiFieldTag.Home
import com.shary.app.core.domain.types.enums.UiFieldTag.Leisure
import com.shary.app.core.domain.types.enums.UiFieldTag.LongTerm
import com.shary.app.core.domain.types.enums.UiFieldTag.Personal
import com.shary.app.core.domain.types.enums.UiFieldTag.Taxes
import com.shary.app.core.domain.types.enums.UiFieldTag.Travel
import com.shary.app.core.domain.types.enums.UiFieldTag.Unknown


sealed class UiFieldTag(val name: String) {
    object Taxes : UiFieldTag("Taxes")
    object Home : UiFieldTag("Home")
    object Personal : UiFieldTag("Personal")
    object Bank : UiFieldTag("Bank")
    object Leisure : UiFieldTag("Leisure")
    object Travel : UiFieldTag("Travel")
    object LongTerm : UiFieldTag("Long-Term")
    object Unknown : UiFieldTag("Unknown")
    data class Custom(val customName: String) : UiFieldTag(customName)

    companion object {
        val entries: List<UiFieldTag> = listOf(
            Taxes, Home, Personal, Bank, Leisure, Travel, LongTerm, Unknown
        )

        // ---------------- Color Options ----------------
        private val DefaultUnknownTagColor = Color.Gray.copy(alpha = 0.7f)
        val TagColorByKey: Map<String, Color> = mapOf(
            "taxes" to Color(0xFF9C27B0),
            "home" to Color(0xFF2196F3),
            "personal" to Color(0xFF4CAF50),
            "bank" to Color(0xFF009688),
            "leisure" to Color(0xFFFF9800),
            "travel" to Color(0xFF3F51B5),
            "long-term" to Color(0xFFE91E63),
            "unknown" to DefaultUnknownTagColor
        )
        fun fromString(tagName: String): UiFieldTag =
            when (tagName.lowercase()) {
                "taxes" -> Taxes
                "home" -> Home
                "personal" -> Personal
                "bank" -> Bank
                "leisure" -> Leisure
                "travel" -> Travel
                "long-term" -> LongTerm
                "unknown" -> Unknown
                else -> Custom(tagName)
            }
    }

    fun UiFieldTag?.safeColor(): Color =
        this?.toColor() ?: DefaultUnknownTagColor


    fun toColor(): Color = TagColorByKey[this.key] ?: DefaultUnknownTagColor
    fun toTagString(): String =
        when (this) {
            is Custom -> this.customName
            else -> this.name
        }
    fun UiFieldTag?.safeTagString(): String =
        this?.toTagString() ?: "Unknown"
}

// stable key for maps
val UiFieldTag.key: String
    get() = when (this) {
        is UiFieldTag.Custom -> customName.trim().lowercase()
        else -> name.trim().lowercase()
    }
/*
// color map by key (works for sealed + custom)
private val DefaultUnknownTagColor = Color.Gray.copy(alpha = 0.7f)
val TagColorByKey: Map<String, Color> = mapOf(
    "taxes" to Color(0xFF9C27B0),
    "home" to Color(0xFF2196F3),
    "personal" to Color(0xFF4CAF50),
    "bank" to Color(0xFF009688),
    "leisure" to Color(0xFFFF9800),
    "travel" to Color(0xFF3F51B5),
    "long-term" to Color(0xFFE91E63),
    "unknown" to DefaultUnknownTagColor
)
*/