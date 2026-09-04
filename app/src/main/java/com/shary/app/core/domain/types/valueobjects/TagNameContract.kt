package com.shary.app.core.domain.types.valueobjects

enum class TagNameInvalidReason {
    BLANK,
    TOO_LONG,
    UNSUPPORTED_CHARACTERS
}

sealed interface TagNameValidation {
    data class Valid(val normalizedName: String) : TagNameValidation
    data class Invalid(val reason: TagNameInvalidReason) : TagNameValidation
}

object TagNameContract {
    const val MAX_LENGTH = 32
    private val allowedCharacters = Regex("""[A-Za-z0-9 _\-\p{L}]+""")

    fun validate(input: String): TagNameValidation {
        val normalized = input.trim().replace(Regex("\\s+"), " ")
        return when {
            normalized.isEmpty() -> TagNameValidation.Invalid(TagNameInvalidReason.BLANK)
            normalized.length > MAX_LENGTH ->
                TagNameValidation.Invalid(TagNameInvalidReason.TOO_LONG)
            !allowedCharacters.matches(normalized) ->
                TagNameValidation.Invalid(TagNameInvalidReason.UNSUPPORTED_CHARACTERS)
            else -> TagNameValidation.Valid(normalized)
        }
    }
}

fun TagNameInvalidReason.defaultMessage(): String = when (this) {
    TagNameInvalidReason.BLANK -> "Tag name is required"
    TagNameInvalidReason.TOO_LONG ->
        "Tag name must be ${TagNameContract.MAX_LENGTH} characters or fewer"
    TagNameInvalidReason.UNSUPPORTED_CHARACTERS ->
        "Use letters, numbers, spaces, hyphens, or underscores"
}
