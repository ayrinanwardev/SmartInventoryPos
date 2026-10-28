package com.example.smartinventorypos.data.repo

import android.content.Context
import androidx.room3.immediateTransaction
import androidx.room3.useWriterConnection

import com.example.smartinventorypos.data.local.database.AppDatabase
import com.example.smartinventorypos.data.local.model.ProductEntity
import com.example.smartinventorypos.data.local.model.ProductReq
import com.example.smartinventorypos.data.local.model.SaleEntity
import com.example.smartinventorypos.data.local.model.SaleItemEntity
import com.example.smartinventorypos.data.remote.ItemReq
import com.example.smartinventorypos.data.remote.LoginReq
import com.example.smartinventorypos.data.remote.PosApi
import com.example.smartinventorypos.data.remote.ProductDto
import com.example.smartinventorypos.data.remote.SaleHistoryDto
import com.example.smartinventorypos.data.remote.SaleReq
import com.example.smartinventorypos.data.remote.Session
import com.example.smartinventorypos.data.remote.StockAdjustReq
import com.example.smartinventorypos.sync.SyncWorker
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException
import java.io.IOException
import java.util.UUID

data class CartLine(val productId: Long, val name: String, val price: Double, val qty: Int)

class PosRepository(
    private val ctx: Context,
    private val db: AppDatabase,
    private val api: PosApi
) {
    private val dao = db.dao()

    // ----- local (Room) -----
    fun search(q: String): Flow<List<ProductEntity>> = dao.search(q.trim())
    fun pendingCount(): Flow<Int> = dao.pendingCount()

    // ----- auth -----
    suspend fun login(user: String, pass: String) {
        val r = api.login(LoginReq(user, pass))
        Session.token = r.token
        Session.role = r.role

        android.util.Log.d("POS_AUTH", "Login role: ${r.role}")
        android.util.Log.d("POS_AUTH", "Token saved: ${!Session.token.isNullOrBlank()}")
    }

    suspend fun register(user: String, pass: String) {
        val r = api.register(LoginReq(user, pass))
        Session.token = r.token
        Session.role = r.role
    }

    // ----- products -----
    /** Skip refresh while unsynced sales exist, otherwise server stock would overwrite local stock. */
    suspend fun refreshProducts() {
        if (dao.pendingCountNow() > 0) return
        runCatching {
            dao.upsertProducts(api.products().map { it.toEntity() })
        }
    }

    suspend fun adminProducts(): List<ProductDto> = api.products()
    suspend fun lowStock(): List<ProductDto> = api.lowStock()

    suspend fun addProduct(name: String, barcode: String, price: Double, stock: Int) {
        val p = api.createProduct(ProductReq(name, barcode.ifBlank { null }, price, stock))
        dao.upsertProducts(listOf(p.toEntity()))
    }

    suspend fun updateProduct(
        id: Long, name: String, barcode: String, price: Double, stock: Int, threshold: Int
    ) {
        val p = api.updateProduct(id, ProductReq(name, barcode.ifBlank { null }, price, stock, threshold))
        dao.upsertProducts(listOf(p.toEntity()))
    }

    suspend fun deleteProduct(id: Long) {
        api.deleteProduct(id)
        dao.deleteProduct(id)
    }

    suspend fun adjustStock(id: Long, delta: Int, reason: String) {
        val p = api.adjustStock(id, StockAdjustReq(delta, reason))
        dao.upsertProducts(listOf(p.toEntity()))
    }

    // ----- sales -----
    suspend fun salesHistory(): List<SaleHistoryDto> = api.salesHistory()
    suspend fun myOrders(): List<SaleHistoryDto> = api.myOrders()
    suspend fun todayTotal(): Double = api.todayTotal()

    /** Always writes locally first (works offline), then schedules background sync. */
    suspend fun checkout(lines: List<CartLine>, discount: Double) {
        val id = UUID.randomUUID().toString()
        val subtotal = lines.sumOf { it.price * it.qty }
        db.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                dao.insertSale(
                    SaleEntity(id, subtotal, discount, subtotal - discount, System.currentTimeMillis())
                )
                dao.insertItems(lines.map {
                    SaleItemEntity(
                        saleClientId = id, productId = it.productId,
                        name = it.name, qty = it.qty, unitPrice = it.price
                    )
                })
                lines.forEach { dao.reduceStock(it.productId, it.qty) }
            }
        }
        SyncWorker.enqueue(ctx)
    }

    /** @return true if everything is synced (or permanently rejected), false if a retry is needed. */
    suspend fun syncPending(): Boolean {
        var allDone = true
        for (s in dao.pendingSales()) {
            try {
                api.createSale(
                    SaleReq(
                        s.sale.clientId,
                        s.items.map { ItemReq(it.productId, it.qty) },
                        s.sale.discount
                    )
                )
                dao.setStatus(s.sale.clientId, 1)
            } catch (e: HttpException) {
                when (e.code()) {
                    400, 404, 409 -> dao.setStatus(s.sale.clientId, 2) // permanent: needs review
                    else -> allDone = false                            // 401/5xx: retry later
                }
            } catch (e: IOException) {
                allDone = false                                        // offline
            }
        }
        return allDone
    }

    private fun ProductDto.toEntity() = ProductEntity(id, name, barcode, price, stock)
}
