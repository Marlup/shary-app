package com.shary.app.infrastructure.connector.remote

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

class RemoteMcpWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            RemoteMcpWorkerEntryPoint::class.java
        )
        return runCatching { entryPoint.coordinator().sync() }
            .fold(onSuccess = { Result.success() }, onFailure = { Result.retry() })
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface RemoteMcpWorkerEntryPoint {
    fun coordinator(): RemoteMcpCoordinator
}

object RemoteMcpWorkScheduler {
    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun schedule(context: Context) {
        val periodic = PeriodicWorkRequestBuilder<RemoteMcpWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodic
        )
        enqueueNow(context)
    }

    fun enqueueNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<RemoteMcpWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_WORK,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(IMMEDIATE_WORK)
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK)
    }

    private const val PERIODIC_WORK = "shary_remote_mcp_periodic"
    private const val IMMEDIATE_WORK = "shary_remote_mcp_immediate"
}
