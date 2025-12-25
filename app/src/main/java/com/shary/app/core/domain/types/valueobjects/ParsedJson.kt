package com.shary.app.core.domain.types.valueobjects

import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.types.enums.DataFileMode
import java.io.File

data class ParsedJson(
    val file: File,
    val fileName: String,
    val mode: DataFileMode?,        // from metadata.mode
    val fields: List<FieldDomain>,  // parsed from fields{}
    val isValidStructure: Boolean
)