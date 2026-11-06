package smart_inventory_backend.stock

import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import smart_inventory_backend.auth.toActor
import smart_inventory_backend.product.ProductResponse

@RestController
class StockController(private val stock: StockService) {

    @PostMapping("/api/products/{id}/stock")
    @PreAuthorize("hasAnyRole('MANAGER','CASHIER')")
    fun adjust(
        @PathVariable id: Long,
        @Valid @RequestBody req: StockAdjustRequest,
        auth: Authentication
    ): ProductResponse = stock.adjust(id, req, auth.toActor())

    @GetMapping("/api/stock/adjustments")
    @PreAuthorize("hasRole('MANAGER')")
    fun history(): List<StockAdjustmentResponse> = stock.history()
}
