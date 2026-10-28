package com.example.smartinventorypos.sync

import android.content.Context
import androidx.compose.ui.unit.Constraints
import androidx.work.*
import androidx.work.WorkManager
import com.example.smartinventorypos.ServiceLocator


class SyncWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result =
        if (ServiceLocator.repo.syncPending()) Result.success() else Result.retry()

    companion object {
        fun enqueue(ctx: Context) {
            val req = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(ctx).enqueueUniqueWork("sale-sync", ExistingWorkPolicy.APPEND_OR_REPLACE, req)
        }
    }
}