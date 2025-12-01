package com.shary.app.core.domain.types.enums

import androidx.compose.ui.graphics.Color

sealed class Tag(open val name: String, open val color: Color?) {

    data class Entry(
        override val name: String,
        override val color: Color
    ) : Tag(name, color)

    data object Unknown : Tag("Unknown", Color.Gray.copy(alpha = 0.7f))

    // ---------------- Helpers ----------------
    fun toTagString(): String = name

    fun toColor(): Color = color ?: Color.Gray.copy(alpha = 0.7f)

    companion object {
        fun fromString(name: String, color: Color?): Tag =
            if (color == null) Unknown else Entry(name.trim(), color)
    }
}

// stable key
val Tag.key: String
    get() = name.trim().lowercase()

fun Tag?.safeColor(): Color = this?.toColor() ?: Color.Gray.copy(alpha = 0.7f)
fun Tag?.safeTagString(): String = this?.toTagString() ?: "Unknown"
