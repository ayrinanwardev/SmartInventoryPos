package com.example.smartinventorypos.domain.usecase

import com.example.smartinventorypos.domain.common.AppResult
import com.example.smartinventorypos.domain.repository.OfflineSaleRepository
import com.example.smartinventorypos.domain.repository.ProductCatalogRepository
import com.example.smartinventorypos.domain.repository.SaleSyncScheduler
import com.example.smartinventorypos.domain.repository.SessionStore
import javax.inject.Inject

/**
 * Updates the local product list from the server. While unsynced sales exist it sends those instead,
 * because server stock would overwrite their local stock changes. The sync refreshes when it finishes.
 */
class RefreshCatalogUseCase @Inject constructor(
    private val sessions: SessionStore,
    private val sales: OfflineSaleRepository,
    private val catalog: ProductCatalogRepository,
    private val syncScheduler: SaleSyncScheduler
) {
    suspend operator fun invoke(): AppResult<Unit> {
        if (sessions.session.value == null) return AppResult.Success(Unit)
        if (sales.hasPending()) {
            syncScheduler.schedule()
            return AppResult.Success(Unit)
        }
        return catalog.refreshFromServer()
    }
}
