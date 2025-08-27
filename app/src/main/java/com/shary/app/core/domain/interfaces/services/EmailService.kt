package com.shary.app.core.domain.interfaces.services

import android.content.Intent
import com.shary.app.core.domain.models.FieldDomain

interface EmailService {
    fun buildEmail(
        records: List<FieldDomain>,
        recipients: List<String>,
        filenameParam: String? = "NULL",
        fileFormat: String = "json"
    ): Result<Intent>
}