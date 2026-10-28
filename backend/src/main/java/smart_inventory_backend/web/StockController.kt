package smart_inventory_backend.web

import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import smart_inventory_backend.model.Product
import smart_inventory_backend.model.StockAdjustment
import smart_inventory_backend.repo.StockAdjustmentRepository
import smart_inventory_backend.security.roleName
import smart_inventory_backend.service.StockAdjustReq
import smart_inventory_backend.service.StockService


@RestController
class StockController(
    private val service: StockService,
    private val log: StockAdjustmentRepository
) {
    /** Manager (unlimited) and cashier (max 20 per change) can adjust stock. */
    @PostMapping("/api/products/{id}/stock")
    @PreAuthorize("hasAnyRole('MANAGER','CASHIER')")
    fun adjust(
        @PathVariable id: Long,
        @Valid @RequestBody req: StockAdjustReq,
        auth: Authentication
    ): Product = service.adjust(id, req, auth.name, auth.roleName())

    @GetMapping("/api/stock/adjustments")
    @PreAuthorize("hasRole('MANAGER')")
    fun history(): List<StockAdjustment> = log.findTop50ByOrderByCreatedAtDesc()
}
