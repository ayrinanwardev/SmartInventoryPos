package com.example.smartinventorypos.domain.usecase

import com.example.smartinventorypos.domain.common.AppResult
import com.example.smartinventorypos.domain.model.SyncStatus
import com.example.smartinventorypos.domain.repository.OfflineSaleRepository
import javax.inject.Inject

class SyncPendingSalesUseCase @Inject constructor(
    private val sales: OfflineSaleRepository,
    private val refreshCatalog: RefreshCatalogUseCase
) {
    /** @return true if every sale was accepted or permanently rejected, false if a retry is needed. */
    suspend operator fun invoke(): Boolean {
        var finished = true
        for (sale in sales.pendingSales()) {
            when (val result = sales.upload(sale)) {
                is AppResult.Success -> sales.markStatus(sale.clientId, SyncStatus.SYNCED)
                is AppResult.Failure ->
                    if (result.error.isPermanentRejection) sales.markStatus(sale.clientId, SyncStatus.REJECTED)
                    else finished = false
            }
        }
        // Picks up the server's stock, which also undoes local stock changes from rejected sales.
        // A failure here doesn't need a retry: the next refresh will catch up.
        if (finished) refreshCatalog()
        return finished
    }
}
