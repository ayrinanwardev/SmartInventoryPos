package smart_inventory_backend.service

import jakarta.validation.constraints.Min

data class ItemReq(
    val productId: Long,
    @field:Min(1) val qty: Int
)