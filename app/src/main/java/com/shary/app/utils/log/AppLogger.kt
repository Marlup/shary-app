package com.shary.app.utils.log

import com.shary.app.BuildConfig

object AppLogger {
    fun error(tag: String, message: String, throwable: Throwable? = null) {
        android.util.Log.e(tag, message, throwable)
    }

    fun warn(tag: String, message: String, throwable: Throwable? = null) {
        android.util.Log.w(tag, message, throwable)
    }

    fun info(tag: String, message: String) {
        if (BuildConfig.DEBUG) android.util.Log.i(tag, message)
    }

    fun debug(tag: String, message: String) {
        if (BuildConfig.DEBUG) android.util.Log.d(tag, message)
    }

    fun redacted(value: String?, head: Int = 4): String {
        if (value.isNullOrBlank()) return "<empty>"
        val prefix = value.take(head)
        return "$prefix…"
    }

    fun emailHint(email: String?): String {
        if (email.isNullOrBlank()) return "<empty>"
        val at = email.indexOf('@')
        return if (at <= 1) "***" else "${email.first()}***${email.substring(at)}"
    }
}
