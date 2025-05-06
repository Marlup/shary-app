package com.shary.app.services.requestField

import com.shary.app.Field
import com.shary.app.core.Session

class RequestFieldService(
    private val session: Session
) {
        fun cacheRequestFields(fields: List<Field>) {
            println("Saving selected fields on stop: $fields")
            session.selectedRequestFields.value = fields
        }
    }