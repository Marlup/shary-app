package com.shary.app.core.domain.types.valueobjects

import com.shary.app.core.domain.models.FieldDomain
import com.shary.app.core.domain.types.enums.DataFileMode

data class VisualizedFile(
    val mode: DataFileMode,
    val fields: List<FieldDomain>
)