package com.example.smartinventorypos.data.remote

data class SaleHistoryDto(
    val id: Long, val clientId: String, val cashier: CashierDto?,
    val subtotal: Double, val discount: Double, val total: Double,
    val createdAt: String, val items: List<SaleLineDto>
)