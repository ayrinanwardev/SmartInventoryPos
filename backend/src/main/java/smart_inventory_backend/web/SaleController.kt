package smart_inventory_backend.web

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import smart_inventory_backend.model.Sale
import smart_inventory_backend.repo.SaleRepository
import smart_inventory_backend.security.roleName
import smart_inventory_backend.service.SaleReq
import smart_inventory_backend.service.SaleService
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId

@RestController
@RequestMapping("/api/sales")
class SaleController(
    private val service: SaleService,
    private val repo: SaleRepository
) {
    /** Customer (buy), cashier and manager (sell). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody req: SaleReq, auth: Authentication): Sale =
        service.checkout(req, auth.name, auth.roleName())

    /** My own orders (customer order history; cashier/manager see what they sold). */
    @GetMapping("/mine")
    fun mine(auth: Authentication): List<Sale> =
        repo.findByCashierUsernameOrderByCreatedAtDesc(auth.name)

    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    fun history(): List<Sale> = repo.findAllByOrderByCreatedAtDesc()

    @GetMapping("/report/today")
    @PreAuthorize("hasRole('MANAGER')")
    fun todayTotal(): BigDecimal {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val from = today.atStartOfDay(zone).toInstant()
        val to = today.plusDays(1).atStartOfDay(zone).toInstant()
        return repo.totalBetween(from, to)
    }
}

