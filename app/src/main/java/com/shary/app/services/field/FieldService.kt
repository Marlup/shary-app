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

    fun fieldToTriple(field: Field): Triple<String, String, String> {
        return Triple(field.key, field.value, field.keyAlias)
    }

    fun valuesToField(key: String, value: String, alias: String="", date: Long=0L): Field {
        return Field
            .newBuilder()
            .setKey(key)
            .setValue(value)
            .setKeyAlias(alias)
            .setDateAdded(date)
            .build()
    }

    fun fieldToPair(field: Field): Pair<String, String> {
        return Pair(field.key, field.value)
    }
}