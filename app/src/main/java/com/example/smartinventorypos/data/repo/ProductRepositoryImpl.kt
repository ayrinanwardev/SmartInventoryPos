package com.example.smartinventorypos.data.repo

import com.example.smartinventorypos.data.local.dao.ProductDao
import com.example.smartinventorypos.data.local.dao.SaleDao
import com.example.smartinventorypos.data.local.database.PosDatabase
import com.example.smartinventorypos.data.mapper.toDomain
import com.example.smartinventorypos.data.mapper.toEntity
import com.example.smartinventorypos.data.mapper.toRequest
import com.example.smartinventorypos.data.remote.PosApi
import com.example.smartinventorypos.data.remote.ProductDto
import com.example.smartinventorypos.data.remote.StockAdjustReq
import com.example.smartinventorypos.domain.common.AppResult
import com.example.smartinventorypos.domain.model.Product
import com.example.smartinventorypos.domain.model.ProductDraft
import com.example.smartinventorypos.domain.model.SyncStatus
import com.example.smartinventorypos.domain.repository.ProductAdminRepository
import com.example.smartinventorypos.domain.repository.ProductCatalogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** One class because both views must keep the local product copy in step with the server. */
class ProductRepositoryImpl @Inject constructor(
    private val db: PosDatabase,
    private val products: ProductDao,
    private val sales: SaleDao,
    private val api: PosApi
) : ProductCatalogRepository, ProductAdminRepository {

    override fun search(query: String): Flow<List<Product>> =
        products.search(query.trim()).map { list -> list.map { it.toDomain() } }

    override suspend fun refreshFromServer(): AppResult<Unit> = safeCall {
        val fresh = api.products().map { it.toEntity() }
        db.writeTransaction {
            // A sale may have been made during the download. Its stock change must not be overwritten.
            if (sales.count(SyncStatus.PENDING) == 0) {
                products.deleteAll()
                products.upsert(fresh)
            }
        }
    }

    override suspend fun fetchAll(): AppResult<List<Product>> = safeCall { api.products().map { it.toDomain() } }

    override suspend fun add(draft: ProductDraft): AppResult<Unit> = saveLocally { api.createProduct(draft.toRequest()) }

    override suspend fun update(id: Long, draft: ProductDraft): AppResult<Unit> =
        saveLocally { api.updateProduct(id, draft.toRequest()) }

    override suspend fun adjustStock(id: Long, delta: Int, reason: String): AppResult<Unit> =
        saveLocally { api.adjustStock(id, StockAdjustReq(delta, reason)) }

    override suspend fun delete(id: Long): AppResult<Unit> = safeCall {
        api.deleteProduct(id)
        products.delete(id)
    }

    private suspend fun saveLocally(request: suspend () -> ProductDto): AppResult<Unit> = safeCall {
        products.upsert(listOf(request().toEntity()))
    }
}
