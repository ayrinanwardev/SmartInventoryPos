package com.example.smartinventorypos.data.repo

import android.content.Context
import androidx.room3.immediateTransaction
import androidx.room3.useWriterConnection
import com.example.smartinventorypos.data.local.database.AppDatabase
import com.example.smartinventorypos.data.local.model.ProductEntity
import com.example.smartinventorypos.data.local.model.SaleEntity
import com.example.smartinventorypos.data.local.model.SaleItemEntity
import com.example.smartinventorypos.data.remote.ItemReq
import com.example.smartinventorypos.data.remote.LoginReq
import com.example.smartinventorypos.data.remote.PosApi
import com.example.smartinventorypos.data.remote.SaleReq
import com.example.smartinventorypos.data.remote.Session
import com.example.smartinventorypos.sync.SyncWorker
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException
import java.io.IOException
import java.util.UUID

data class CartLine(val productId: Long, val name: String, val price: Double, val qty: Int)

class PosRepository(
    private val ctx: Context, private val db: AppDatabase, private val api: PosApi
) {
    private val dao = db.dao()

    fun search(q: String): Flow<List<ProductEntity>> = dao.search(q.trim())
    fun pendingCount(): Flow<Int> = dao.pendingCount()

    suspend fun login(user: String, pass: String) {
        val r = api.login(LoginReq(user, pass))
        Session.token = r.token; Session.role = r.role
    }

    /** Skip refresh while unsynced sales exist, otherwise server stock would overwrite local stock. */
    suspend fun refreshProducts() {
        if (dao.pendingCountNow() > 0) return
        runCatching {
            dao.upsertProducts(api.products().map {
                ProductEntity(
                    it.id,
                    it.name,
                    it.barcode,
                    it.price,
                    it.stock
                )
            })
        }
    }

//    Unresolved reference 'withTransaction' on receiver of type 'AppDatabase'.
    /** @return true if everything is synced (or permanently rejected), false if a retry is needed. */
    suspend fun syncPending(): Boolean {
        var allDone = true
        for (s in dao.pendingSales()) {
            try {
                api.createSale(SaleReq(s.sale.clientId, s.items.map {
                    ItemReq(
                        it.productId,
                        it.qty
                    )
                }, s.sale.discount))
                dao.setStatus(s.sale.clientId, 1)
            } catch (e: HttpException) {
                when (e.code()) {
                    400, 404, 409 -> dao.setStatus(s.sale.clientId, 2) // permanent: needs manager review
                    else -> allDone = false                            // 401/5xx: retry later
                }
            } catch (e: IOException) { allDone = false }               // offline
        }
        return allDone
    }

    suspend fun checkout(lines: List<CartLine>, discount: Double) {
        val id = UUID.randomUUID().toString()
        val subtotal = lines.sumOf { it.price * it.qty }
        db.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                dao.insertSale(
                    SaleEntity(
                        id,
                        subtotal,
                        discount,
                        subtotal - discount,
                        System.currentTimeMillis()
                    )
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
}
