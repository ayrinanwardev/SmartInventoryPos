package com.example.smartinventorypos.data.local.model

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.example.smartinventorypos.domain.model.SyncStatus

@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey val clientId: String,
    val subtotal: Double, val discount: Double, val total: Double,
    val createdAt: Long, val status: SyncStatus = SyncStatus.PENDING
)
