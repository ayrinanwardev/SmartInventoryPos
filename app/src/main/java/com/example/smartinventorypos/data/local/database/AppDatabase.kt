package com.example.smartinventorypos.data.local.database

import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.example.smartinventorypos.data.local.dao.PosDao
import com.example.smartinventorypos.data.local.model.ProductEntity
import com.example.smartinventorypos.data.local.model.SaleEntity
import com.example.smartinventorypos.data.local.model.SaleItemEntity

@Database(entities = [ProductEntity::class, SaleEntity::class, SaleItemEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() { abstract fun dao(): PosDao }