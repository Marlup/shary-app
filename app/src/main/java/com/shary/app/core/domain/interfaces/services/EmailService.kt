package com.shary.app.core.domain.interfaces.services

import com.shary.app.core.domain.models.FieldDomain

interface EmailService {
    fun sendEmail(
        records: List<FieldDomain>,
        recipients: List<String>,
        filenameParam: String? = "SharyFile",
        fileFormat: String = "json"
    ): Boolean
}