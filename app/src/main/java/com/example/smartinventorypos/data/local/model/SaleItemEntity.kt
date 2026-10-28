package com.example.smartinventorypos.data.local.model

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey


@Entity(
    tableName = "sale_items", indices = [Index("saleClientId")],
    foreignKeys = [ForeignKey(
        entity = SaleEntity::class, parentColumns = ["clientId"],
        childColumns = ["saleClientId"], onDelete = ForeignKey.CASCADE
    )]
)
data class SaleItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleClientId: String, val productId: Long, val name: String, val qty: Int, val unitPrice: Double
)

