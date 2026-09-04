package com.shary.app.utils.log

import android.os.SystemClock
import java.util.concurrent.atomic.AtomicLong

object StartupTrace {
    private val loginStartElapsedMs = AtomicLong(0L)

    fun markLoginStart() {
        loginStartElapsedMs.set(SystemClock.elapsedRealtime())
    }

    fun elapsedSinceLoginStartMsOrNull(): Long? {
        val start = loginStartElapsedMs.get()
        if (start <= 0L) return null
        return (SystemClock.elapsedRealtime() - start).coerceAtLeast(0L)
    }
}
