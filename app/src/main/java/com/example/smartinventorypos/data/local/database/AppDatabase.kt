package com.example.smartinventorypos.data.local.database

import androidx.room3.ColumnTypeConverters
import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.example.smartinventorypos.data.local.dao.ProductDao
import com.example.smartinventorypos.data.local.dao.SaleDao
import com.example.smartinventorypos.data.local.model.ProductEntity
import com.example.smartinventorypos.data.local.model.SaleEntity
import com.example.smartinventorypos.data.local.model.SaleItemEntity

@Database(entities = [ProductEntity::class, SaleEntity::class, SaleItemEntity::class], version = 1)
@ColumnTypeConverters(SyncStatusConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun saleDao(): SaleDao
}
