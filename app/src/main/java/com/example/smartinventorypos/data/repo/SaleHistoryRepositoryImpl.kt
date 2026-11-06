package com.example.smartinventorypos.data.repo

import com.example.smartinventorypos.data.mapper.toDomain
import com.example.smartinventorypos.data.remote.PosApi
import com.example.smartinventorypos.domain.common.AppResult
import com.example.smartinventorypos.domain.model.Money
import com.example.smartinventorypos.domain.model.SaleRecord
import com.example.smartinventorypos.domain.repository.SaleHistoryRepository
import javax.inject.Inject

class SaleHistoryRepositoryImpl @Inject constructor(private val api: PosApi) : SaleHistoryRepository {
    override suspend fun allSales(): AppResult<List<SaleRecord>> = safeCall { api.salesHistory().map { it.toDomain() } }
    override suspend fun myOrders(): AppResult<List<SaleRecord>> = safeCall { api.myOrders().map { it.toDomain() } }
    override suspend fun todayTotal(): AppResult<Money> = safeCall { Money.fromDouble(api.todayTotal()) }
}
