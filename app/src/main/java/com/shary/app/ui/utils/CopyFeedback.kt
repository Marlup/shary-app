package com.shary.app.ui.utils

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper

object CopyFeedback {
    private val handler = Handler(Looper.getMainLooper())

    fun playSuccessTone() {
        runCatching {
            val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 45)
            try {
                tone.startTone(ToneGenerator.TONE_PROP_ACK, 65)
            } finally {
                // Release after the tone has finished playing. Releasing
                // immediately truncates the audio track and logs
                // "Stop timed out" / "stop called with N frames delivered".
                handler.postDelayed({ tone.release() }, 150L)
            }
        }
    }
}
