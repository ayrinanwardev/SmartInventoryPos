package smart_inventory_backend.service

import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotEmpty
import java.math.BigDecimal
import java.util.UUID

data class SaleReq(
    val clientId: UUID,
    @field:NotEmpty @field:Valid val items: List<ItemReq>,
    @field:DecimalMin("0.0") val discount: BigDecimal? = null
)