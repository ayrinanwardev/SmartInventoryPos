package com.example.smartinventorypos.data.mapper

import com.example.smartinventorypos.data.local.model.ProductEntity
import com.example.smartinventorypos.data.local.model.SaleWithItems
import com.example.smartinventorypos.data.remote.ItemReq
import com.example.smartinventorypos.data.remote.ProductDto
import com.example.smartinventorypos.data.remote.ProductReq
import com.example.smartinventorypos.data.remote.SaleHistoryDto
import com.example.smartinventorypos.data.remote.SaleLineDto
import com.example.smartinventorypos.data.remote.SaleReq
import com.example.smartinventorypos.domain.model.Money
import com.example.smartinventorypos.domain.model.PendingSale
import com.example.smartinventorypos.domain.model.PendingSaleLine
import com.example.smartinventorypos.domain.model.Product
import com.example.smartinventorypos.domain.model.ProductDraft
import com.example.smartinventorypos.domain.model.SaleRecord
import com.example.smartinventorypos.domain.model.SaleRecordLine

fun ProductDto.toDomain() = Product(id, name, barcode, Money.fromDouble(price), stock, lowStockThreshold)

fun ProductDto.toEntity() = ProductEntity(id, name, barcode, price, stock)

fun ProductEntity.toDomain() = Product(id, name, barcode, Money.fromDouble(price), stock)

fun ProductDraft.toRequest() = ProductReq(name, barcode, price.toDouble(), stock, lowStockThreshold)

fun SaleHistoryDto.toDomain() = SaleRecord(
    id = id,
    cashierName = cashier?.username,
    discount = Money.fromDouble(discount),
    total = Money.fromDouble(total),
    createdAt = createdAt,
    lines = items.map { it.toDomain() }
)

private fun SaleLineDto.toDomain() = SaleRecordLine(product?.name, qty, Money.fromDouble(unitPrice))

fun SaleWithItems.toDomain() = PendingSale(
    clientId = sale.clientId,
    lines = items.map { PendingSaleLine(it.productId, it.qty) },
    discount = Money.fromDouble(sale.discount)
)

fun PendingSale.toRequest() = SaleReq(clientId, lines.map { ItemReq(it.productId, it.quantity) }, discount.toDouble())
