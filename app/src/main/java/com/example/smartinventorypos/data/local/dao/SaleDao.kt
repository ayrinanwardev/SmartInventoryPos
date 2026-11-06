package com.example.smartinventorypos.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import com.example.smartinventorypos.data.local.model.SaleEntity
import com.example.smartinventorypos.data.local.model.SaleItemEntity
import com.example.smartinventorypos.data.local.model.SaleWithItems
import com.example.smartinventorypos.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {
    @Insert
    suspend fun insertSale(sale: SaleEntity)

    @Insert
    suspend fun insertItems(items: List<SaleItemEntity>)

    @Transaction
    @Query("SELECT * FROM sales WHERE status = :status ORDER BY createdAt")
    suspend fun salesWithStatus(status: SyncStatus): List<SaleWithItems>

    @Query("UPDATE sales SET status = :status WHERE clientId = :id")
    suspend fun setStatus(id: String, status: SyncStatus)

    @Query("UPDATE sales SET status = :to WHERE status = :from")
    suspend fun moveStatus(from: SyncStatus, to: SyncStatus)

    @Query("SELECT COUNT(*) FROM sales WHERE status = :status")
    fun observeCount(status: SyncStatus): Flow<Int>

    @Query("SELECT COUNT(*) FROM sales WHERE status = :status")
    suspend fun count(status: SyncStatus): Int
}
