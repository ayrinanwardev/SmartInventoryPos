package smart_inventory_backend.service

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class StockAdjustReq(
    val delta: Int,                                   // +10 = restock, -2 = damaged/lost
    @field:NotBlank @field:Size(min = 3) val reason: String
)