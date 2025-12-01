package com.shary.app.core.domain.interfaces.states

import com.shary.app.core.domain.types.enums.FileType
import java.io.File


data class ParsedZip(
    val file: File,                     // Local copy of the original ZIP (private storage)
    val fileName: String,               // Name of that file
    val type: FileType = FileType.NONE, // Parsed from meta.txt (e.g., "type=request" or "type=response")
    val fields: Map<String, String>,    // Parsed from content.json -> fields{}
    val isValidStructure: Boolean       // Result of ZIP structure validation
)

