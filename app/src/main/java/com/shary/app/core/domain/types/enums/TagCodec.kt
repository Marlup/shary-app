// utils/TagCodec.kt
package com.shary.app.core.domain.types.enums

import androidx.compose.ui.graphics.Color

private fun Color.toArgbInt(): Int =
    ((alpha * 255).toInt() shl 24) or
            ((red   * 255).toInt() shl 16) or
            ((green * 255).toInt() shl 8)  or
            ((blue  * 255).toInt())

private fun Int.toColor(): Color =
    Color(
        ((this shr 16) and 0xFF) / 255f,
        ((this shr 8)  and 0xFF) / 255f,
        ( this        and 0xFF) / 255f,
        ((this shr 24) and 0xFF) / 255f
    )

fun Tag.serialize(): String {
    val argb = (toColor().toArgbInt())
    val hex = String.format("#%08X", argb) // AARRGGBB
    return "${toTagString()}|$hex"
}

fun Tag.Companion.deserialize(raw: String): Tag {
    // Retro compatibility: if there's not '|', only name → color by default
    val parts = raw.split('|', limit = 2)
    return if (parts.size == 2) {
        val name = parts[0]
        val hex  = parts[1]
        val argb = hex.removePrefix("#").toLong(16).toInt()
        Tag.fromString(name, argb.toColor())
    } else {
        Tag.fromString(raw, Tag.Unknown.toColor())
    }
}
