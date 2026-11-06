package smart_inventory_backend.product

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal
import java.time.Instant

data class ProductRequest(
    @field:NotBlank val name: String = "",
    val barcode: String? = null,
    @field:DecimalMin("0.0") val price: BigDecimal = BigDecimal.ZERO,
    @field:Min(0) val stock: Int = 0,
    val lowStockThreshold: Int = 5
)

data class ProductResponse(
    val id: Long,
    val name: String,
    val barcode: String?,
    val price: BigDecimal,
    val stock: Int,
    val lowStockThreshold: Int,
    val active: Boolean,
    val updatedAt: Instant
)

fun Product.toResponse() = ProductResponse(
    id = requireNotNull(id) { "Product has not been saved" },
    name = name,
    barcode = barcode,
    price = price,
    stock = stock,
    lowStockThreshold = lowStockThreshold,
    active = active,
    updatedAt = updatedAt
)
