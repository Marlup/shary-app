package com.shary.app.core.domain.types.enums

enum class DataFileMode {
    Response,
    Request;

    companion object {
        /** Parse a raw string (e.g. from meta.txt) into an enum. */
        fun fromString(raw: String?): DataFileMode? {
            return when (raw?.trim()?.lowercase()) {
                "response" -> Response
                "request"  -> Request
                else       -> null
            }
        }

        /** Convert enum back to string (for saving/writing meta.txt). */
        fun toString(mode: DataFileMode): String {
            return when (mode) {
                Response -> "response"
                Request  -> "request"
            }
        }
    }
}