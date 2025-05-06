package com.shary.app.utils

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale


object DateUtils {

    @SuppressLint("NewApi")
    fun formatTimeMillis(timeMillis: Long): String {
        val instant = Instant.ofEpochMilli(timeMillis)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault())
        return formatter.format(instant)
    }
    fun getCurrentFormattedDate(): String {
        return formatTimeMillis(System.currentTimeMillis())
    }
}
