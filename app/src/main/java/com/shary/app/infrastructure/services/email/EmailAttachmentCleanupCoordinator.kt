package com.shary.app.infrastructure.services.email

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Provides process-local timeout cleanup and restart recovery for email
 * attachments. The on-disk lease remains authoritative across process death.
 */
class EmailAttachmentCleanupCoordinator(
    private val store: EmailAttachmentStore,
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    private val started = AtomicBoolean(false)
    private val jobs = mutableMapOf<String, Job>()

    fun start() {
        if (!started.compareAndSet(false, true)) return
        scope.launch { sweepExpiredAndReschedule() }
    }

    fun sweepExpiredAndReschedule(): Int {
        val deleted = store.sweepExpired()
        store.listActiveLeases().forEach(::schedule)
        return deleted
    }

    fun schedule(lease: EmailAttachmentLease): Boolean {
        if (store.findLease(lease.attachmentId) == null) return false

        synchronized(jobs) {
            jobs.remove(lease.attachmentId)?.cancel()
            lateinit var cleanupJob: Job
            cleanupJob = scope.launch {
                delay((lease.expiresAtEpochMillis - nowMillis()).coerceAtLeast(0L))
                while (!store.delete(lease.attachmentId)) {
                    delay(DELETE_RETRY_MILLIS)
                }
                synchronized(jobs) {
                    if (jobs[lease.attachmentId] === cleanupJob) {
                        jobs.remove(lease.attachmentId)
                    }
                }
            }
            jobs[lease.attachmentId] = cleanupJob
        }
        return true
    }

    fun deleteNow(attachmentId: String): Boolean {
        val lease = store.findLease(attachmentId)
        synchronized(jobs) {
            jobs.remove(attachmentId)?.cancel()
        }
        val deleted = store.delete(attachmentId)
        if (!deleted && lease != null) schedule(lease)
        return deleted
    }

    internal fun stopForTest() {
        synchronized(jobs) {
            jobs.values.forEach(Job::cancel)
            jobs.clear()
        }
    }

    private companion object {
        const val DELETE_RETRY_MILLIS = 60_000L
    }
}
