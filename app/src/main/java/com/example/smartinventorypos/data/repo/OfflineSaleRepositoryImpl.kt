package com.example.smartinventorypos.data.repo

import com.example.smartinventorypos.data.local.dao.ProductDao
import com.example.smartinventorypos.data.local.dao.SaleDao
import com.example.smartinventorypos.data.local.database.PosDatabase
import com.example.smartinventorypos.data.local.model.SaleEntity
import com.example.smartinventorypos.data.local.model.SaleItemEntity
import com.example.smartinventorypos.data.mapper.toDomain
import com.example.smartinventorypos.data.mapper.toRequest
import com.example.smartinventorypos.data.remote.PosApi
import com.example.smartinventorypos.domain.common.AppResult
import com.example.smartinventorypos.domain.model.Cart
import com.example.smartinventorypos.domain.model.Money
import com.example.smartinventorypos.domain.model.PendingSale
import com.example.smartinventorypos.domain.model.SyncStatus
import com.example.smartinventorypos.domain.repository.OfflineSaleRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

class OfflineSaleRepositoryImpl @Inject constructor(
    private val db: PosDatabase,
    private val sales: SaleDao,
    private val products: ProductDao,
    private val api: PosApi
) : OfflineSaleRepository {

    override fun pendingCount(): Flow<Int> = sales.observeCount(SyncStatus.PENDING)
    override fun rejectedCount(): Flow<Int> = sales.observeCount(SyncStatus.REJECTED)
    override suspend fun hasPending(): Boolean = sales.count(SyncStatus.PENDING) > 0

    override suspend fun acknowledgeRejected() =
        sales.moveStatus(SyncStatus.REJECTED, SyncStatus.REJECTED_ACKNOWLEDGED)

    override suspend fun record(cart: Cart, discount: Money): AppResult<Unit> = safeCall {
        val clientId = UUID.randomUUID().toString()
        val subtotal = cart.subtotal
        val sale = SaleEntity(
            clientId = clientId,
            subtotal = subtotal.toDouble(),
            discount = discount.toDouble(),
            total = (subtotal - discount).toDouble(),
            createdAt = System.currentTimeMillis()
        )
        val items = cart.lines.map {
            SaleItemEntity(
                saleClientId = clientId,
                productId = it.productId,
                name = it.name,
                qty = it.quantity,
                unitPrice = it.unitPrice.toDouble()
            )
        }
        db.writeTransaction {
            sales.insertSale(sale)
            sales.insertItems(items)
            cart.lines.forEach { products.reduceStock(it.productId, it.quantity) }
        }
    }

    override suspend fun pendingSales(): List<PendingSale> =
        sales.salesWithStatus(SyncStatus.PENDING).map { it.toDomain() }

    override suspend fun upload(sale: PendingSale): AppResult<Unit> = safeCall {
        api.createSale(sale.toRequest())
        Unit
    }

    override suspend fun markStatus(clientId: String, status: SyncStatus) = sales.setStatus(clientId, status)
}
