package com.example.smartinventorypos.domain.model

/** Where a locally saved sale is in its journey to the server. */
enum class SyncStatus { PENDING, SYNCED, REJECTED, REJECTED_ACKNOWLEDGED }

/** A sale saved on this device and waiting to be sent. */
data class PendingSale(val clientId: String, val lines: List<PendingSaleLine>, val discount: Money)

data class PendingSaleLine(val productId: Long, val quantity: Int)

/** A completed sale as the server reports it. */
data class SaleRecord(
    val id: Long,
    val cashierName: String?,
    val discount: Money,
    val total: Money,
    val createdAt: String,
    val lines: List<SaleRecordLine>
)

data class SaleRecordLine(val productName: String?, val quantity: Int, val unitPrice: Money)
