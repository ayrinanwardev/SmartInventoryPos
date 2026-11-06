package smart_inventory_backend.sale

import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import smart_inventory_backend.auth.AppUser
import smart_inventory_backend.auth.Role
import smart_inventory_backend.product.ProductResponse
import smart_inventory_backend.product.toResponse
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class SaleItemRequest(
    val productId: Long,
    @field:Min(1) val qty: Int
)

data class SaleRequest(
    val clientId: UUID,
    @field:NotEmpty @field:Valid val items: List<SaleItemRequest>,
    @field:DecimalMin("0.0") val discount: BigDecimal? = null
)

data class CashierResponse(val id: Long, val username: String, val role: Role)

data class SaleItemResponse(
    val id: Long,
    val product: ProductResponse,
    val qty: Int,
    val unitPrice: BigDecimal
)

data class SaleResponse(
    val id: Long,
    val clientId: UUID,
    val cashier: CashierResponse?,
    val subtotal: BigDecimal,
    val discount: BigDecimal,
    val total: BigDecimal,
    val createdAt: Instant,
    val items: List<SaleItemResponse>
)

fun Sale.toResponse() = SaleResponse(
    id = requireNotNull(id) { "Sale has not been saved" },
    clientId = clientId,
    cashier = cashier?.toCashierResponse(),
    subtotal = subtotal,
    discount = discount,
    total = total,
    createdAt = createdAt,
    items = items.map { it.toResponse() }
)

private fun SaleItem.toResponse() = SaleItemResponse(
    id = requireNotNull(id) { "Sale item has not been saved" },
    product = requireNotNull(product) { "Sale item has no product" }.toResponse(),
    qty = qty,
    unitPrice = unitPrice
)

private fun AppUser.toCashierResponse() = CashierResponse(
    id = requireNotNull(id) { "User has not been saved" },
    username = username,
    role = role
)
