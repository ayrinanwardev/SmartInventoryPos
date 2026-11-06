package com.example.smartinventorypos.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.example.smartinventorypos.data.local.model.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE name LIKE '%' || :q || '%' OR barcode = :q ORDER BY name")
    fun search(q: String): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(products: List<ProductEntity>)

    @Query("UPDATE products SET stock = stock - :qty WHERE id = :id")
    suspend fun reduceStock(id: Long, qty: Int)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM products")
    suspend fun deleteAll()
}
