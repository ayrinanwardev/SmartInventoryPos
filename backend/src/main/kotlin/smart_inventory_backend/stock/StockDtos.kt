package smart_inventory_backend.stock

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import smart_inventory_backend.auth.Role
import smart_inventory_backend.product.ProductResponse
import smart_inventory_backend.product.toResponse
import java.time.Instant

data class StockAdjustRequest(
    val delta: Int,                                   // +10 = restock, -2 = damaged/lost
    @field:NotBlank @field:Size(min = 3) val reason: String
)

data class StockAdjustmentResponse(
    val id: Long,
    val product: ProductResponse,
    val delta: Int,
    val stockAfter: Int,
    val reason: String,
    val performedBy: String,
    val role: Role,
    val createdAt: Instant
)

fun StockAdjustment.toResponse() = StockAdjustmentResponse(
    id = requireNotNull(id) { "Stock adjustment has not been saved" },
    product = requireNotNull(product) { "Stock adjustment has no product" }.toResponse(),
    delta = delta,
    stockAfter = stockAfter,
    reason = reason,
    performedBy = performedBy,
    role = role,
    createdAt = createdAt
)
