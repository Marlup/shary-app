package com.shary.app.infrastructure.services.clipboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.shary.app.core.domain.interfaces.services.ClipboardService
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal const val SHARY_CLIP_TOKEN_EXTRA = "com.shary.app.clipboard.CLIP_TOKEN"
internal const val ANDROID_SENSITIVE_CLIP_EXTRA = "android.content.extra.IS_SENSITIVE"

internal fun interface ScheduledClipboardClear {
    fun cancel()
}

internal fun interface ClipboardClearScheduler {
    fun schedule(delayMillis: Long, action: () -> Unit): ScheduledClipboardClear
}

internal interface ClipboardGateway {
    fun putText(label: String, text: String, sensitive: Boolean, token: String)
    fun clearIfOwned(token: String): Boolean
}

private class CoroutineClipboardClearScheduler : ClipboardClearScheduler {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun schedule(
        delayMillis: Long,
        action: () -> Unit
    ): ScheduledClipboardClear {
        val job: Job = scope.launch {
            delay(delayMillis)
            action()
        }
        return ScheduledClipboardClear(job::cancel)
    }
}

private class AndroidClipboardGateway(context: Context) : ClipboardGateway {
    private val clipboard = context.applicationContext
        .getSystemService(ClipboardManager::class.java)

    override fun putText(label: String, text: String, sensitive: Boolean, token: String) {
        val clip = ClipData.newPlainText(label, text)
        clip.description.extras = PersistableBundle().apply {
            putString(SHARY_CLIP_TOKEN_EXTRA, token)
            if (sensitive) putBoolean(ANDROID_SENSITIVE_CLIP_EXTRA, true)
        }
        clipboard.setPrimaryClip(clip)
    }

    override fun clearIfOwned(token: String): Boolean {
        val currentToken = clipboard.primaryClipDescription
            ?.extras
            ?.getString(SHARY_CLIP_TOKEN_EXTRA)
        if (currentToken != token) return false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            clipboard.clearPrimaryClip()
        } else {
            clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
        }
        return true
    }
}

@Singleton
class ClipboardCleanupCoordinator internal constructor(
    private val gateway: ClipboardGateway,
    private val scheduler: ClipboardClearScheduler
) : ClipboardService, DefaultLifecycleObserver {

    @Inject
    constructor(@ApplicationContext context: Context) : this(
        gateway = AndroidClipboardGateway(context),
        scheduler = CoroutineClipboardClearScheduler()
    )

    private var activeToken: String? = null
    private var pendingClear: ScheduledClipboardClear? = null
    private var observing = false

    fun start() {
        if (observing) return
        observing = true
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun copyText(
        label: String,
        text: String,
        sensitive: Boolean,
        clearAfterSeconds: Int
    ) {
        pendingClear?.cancel()
        val token = UUID.randomUUID().toString()
        activeToken = token
        gateway.putText(label = label, text = text, sensitive = sensitive, token = token)

        pendingClear = if (clearAfterSeconds > 0) {
            scheduler.schedule(clearAfterSeconds * 1_000L) { clearToken(token) }
        } else {
            null
        }
    }

    override fun clearOwnedClip() {
        pendingClear?.cancel()
        pendingClear = null
        activeToken?.let(gateway::clearIfOwned)
        activeToken = null
    }

    override fun onStop(owner: LifecycleOwner) {
        clearOwnedClip()
    }

    private fun clearToken(token: String) {
        if (activeToken != token) return
        gateway.clearIfOwned(token)
        activeToken = null
        pendingClear = null
    }
}
