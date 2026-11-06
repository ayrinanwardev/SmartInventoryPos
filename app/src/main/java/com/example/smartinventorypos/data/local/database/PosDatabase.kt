package com.example.smartinventorypos.data.local.database

import android.content.Context
import androidx.room3.Room
import androidx.room3.immediateTransaction
import androidx.room3.useWriterConnection
import com.example.smartinventorypos.data.local.dao.ProductDao
import com.example.smartinventorypos.data.local.dao.SaleDao
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** The app's access to Room: its DAOs and write transactions, so repositories don't use Room's connection API. */
@Singleton
class PosDatabase @Inject constructor(@ApplicationContext context: Context) {
    private val database: AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "pos.db").build()

    fun productDao(): ProductDao = database.productDao()
    fun saleDao(): SaleDao = database.saleDao()

    suspend fun <T> writeTransaction(block: suspend () -> T): T =
        database.useWriterConnection { transactor ->
            transactor.immediateTransaction { block() }
        }
}
