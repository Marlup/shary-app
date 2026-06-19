package com.shary.app.ui.utils

import android.media.AudioManager
import android.media.ToneGenerator

object CopyFeedback {
    fun playSuccessTone() {
        runCatching {
            val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 45)
            try {
                tone.startTone(ToneGenerator.TONE_PROP_ACK, 65)
            } finally {
                tone.release()
            }
        }
    }
}
