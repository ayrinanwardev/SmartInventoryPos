package com.example.smartinventorypos.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.smartinventorypos.domain.usecase.SyncPendingSalesUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** WorkManager stores this class's name in queued work, so renaming or moving it would strand that work. */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncPendingSales: SyncPendingSalesUseCase
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = if (syncPendingSales()) Result.success() else Result.retry()
}
