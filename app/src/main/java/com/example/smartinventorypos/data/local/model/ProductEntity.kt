package com.example.smartinventorypos.data.local.model

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: Long, val name: String, val barcode: String?, val price: Double, val stock: Int
)