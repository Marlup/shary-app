package com.shary.app.services.field

import com.shary.app.Field
import com.shary.app.core.Session

class FieldService(
    private val session: Session
) {
    fun cacheSelectedFields(fields: List<Field>) {
        println("Saving selected keys on stop: $fields")
        session.selectedFields.value = fields
    }
}