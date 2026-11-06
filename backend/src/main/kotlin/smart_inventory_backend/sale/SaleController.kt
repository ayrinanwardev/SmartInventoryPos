package smart_inventory_backend.sale

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import smart_inventory_backend.auth.toActor
import java.math.BigDecimal

@RestController
@RequestMapping("/api/sales")
class SaleController(
    private val sales: SaleService,
    private val reports: ReportService
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody req: SaleRequest, auth: Authentication): SaleResponse =
        sales.checkout(req, auth.toActor())

    /** The caller's own sales: order history for customers, what they sold for staff. */
    @GetMapping("/mine")
    fun mine(
        auth: Authentication,
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) size: Int?
    ): List<SaleResponse> = sales.salesBy(auth.name, page, size)

    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    fun history(
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) size: Int?
    ): List<SaleResponse> = sales.history(page, size)

    @GetMapping("/report/today")
    @PreAuthorize("hasRole('MANAGER')")
    fun todayTotal(): BigDecimal = reports.todayTotal()
}
