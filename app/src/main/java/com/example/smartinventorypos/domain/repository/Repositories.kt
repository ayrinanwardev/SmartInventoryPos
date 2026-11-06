package com.example.smartinventorypos.domain.repository

import com.example.smartinventorypos.domain.common.AppResult
import com.example.smartinventorypos.domain.model.Cart
import com.example.smartinventorypos.domain.model.Money
import com.example.smartinventorypos.domain.model.PendingSale
import com.example.smartinventorypos.domain.model.Product
import com.example.smartinventorypos.domain.model.ProductDraft
import com.example.smartinventorypos.domain.model.SaleRecord
import com.example.smartinventorypos.domain.model.SyncStatus
import com.example.smartinventorypos.domain.model.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** The signed-in user, kept across app restarts. */
interface SessionStore {
    val session: StateFlow<UserSession?>
    fun save(session: UserSession)
    fun clear()

    /** Ends the session only if it still uses this token, so a late 401 can't sign out a newer login. */
    fun clearIfCurrent(token: String)
}

interface AuthRepository {
    suspend fun login(username: String, password: String): AppResult<Unit>
    suspend fun register(username: String, password: String): AppResult<Unit>
    fun logout()
}

/** The products available to sell, read from the local copy so selling works offline. */
interface ProductCatalogRepository {
    fun search(query: String): Flow<List<Product>>

    /** Replaces the local copy with the server's, unless unsynced sales exist. */
    suspend fun refreshFromServer(): AppResult<Unit>
}

/** Online product management. Changes also update the local copy. */
interface ProductAdminRepository {
    suspend fun fetchAll(): AppResult<List<Product>>
    suspend fun add(draft: ProductDraft): AppResult<Unit>
    suspend fun update(id: Long, draft: ProductDraft): AppResult<Unit>
    suspend fun delete(id: Long): AppResult<Unit>
    suspend fun adjustStock(id: Long, delta: Int, reason: String): AppResult<Unit>
}

/** Sales made on this device, saved first and sent later. */
interface OfflineSaleRepository {
    fun pendingCount(): Flow<Int>
    fun rejectedCount(): Flow<Int>
    suspend fun hasPending(): Boolean
    suspend fun acknowledgeRejected()
    suspend fun record(cart: Cart, discount: Money): AppResult<Unit>
    suspend fun pendingSales(): List<PendingSale>
    suspend fun upload(sale: PendingSale): AppResult<Unit>
    suspend fun markStatus(clientId: String, status: SyncStatus)
}

interface SaleHistoryRepository {
    suspend fun allSales(): AppResult<List<SaleRecord>>
    suspend fun myOrders(): AppResult<List<SaleRecord>>
    suspend fun todayTotal(): AppResult<Money>
}

/** Asks for unsynced sales to be sent in the background when a connection is available. */
interface SaleSyncScheduler {
    fun schedule()
}
