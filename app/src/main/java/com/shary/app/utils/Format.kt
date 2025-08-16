package com.shary.app.utils

import com.shary.app.Field

object FormattingUtils {

    fun makeKeyValueTextFromFields(fields: List<Field>): String {
        return fields.joinToString("\n") { "${it.key}: ${it.value}" }
    }
}