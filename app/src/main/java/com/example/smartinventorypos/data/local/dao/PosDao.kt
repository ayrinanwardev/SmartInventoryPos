package com.example.smartinventorypos.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import com.example.smartinventorypos.data.local.model.ProductEntity
import com.example.smartinventorypos.data.local.model.SaleEntity
import com.example.smartinventorypos.data.local.model.SaleItemEntity
import com.example.smartinventorypos.data.local.model.SaleWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface PosDao {
    @Query("SELECT * FROM products WHERE name LIKE '%' || :q || '%' OR barcode = :q ORDER BY name")
    fun search(q: String): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertProducts(list: List<ProductEntity>)
    @Query("UPDATE products SET stock = stock - :qty WHERE id = :id") suspend fun reduceStock(id: Long, qty: Int)

    @Insert suspend fun insertSale(s: SaleEntity)
    @Insert
    suspend fun insertItems(items: List<SaleItemEntity>)

    @Transaction
    @Query("SELECT * FROM sales WHERE status = 0 ORDER BY createdAt")
    suspend fun pendingSales(): List<SaleWithItems>

    @Query("UPDATE sales SET status = :status WHERE clientId = :id") suspend fun setStatus(id: String, status: Int)
    @Query("SELECT COUNT(*) FROM sales WHERE status = 0") fun pendingCount(): Flow<Int>
    @Query("SELECT COUNT(*) FROM sales WHERE status = 0") suspend fun pendingCountNow(): Int

    @Transaction @Query("SELECT * FROM sales ORDER BY createdAt DESC")
    fun history(): Flow<List<SaleWithItems>>

    @Query("DELETE FROM products WHERE id = :id") suspend fun deleteProduct(id: Long)
}