package smart_inventory_backend.sale

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate

@Service
class ReportService(
    private val sales: SaleRepository,
    private val clock: Clock
) {
    /** Total sales for the current calendar day in the server's time zone. */
    @Transactional(readOnly = true)
    fun todayTotal(): BigDecimal {
        val today = LocalDate.now(clock)
        val from = today.atStartOfDay(clock.zone).toInstant()
        val to = today.plusDays(1).atStartOfDay(clock.zone).toInstant()
        return sales.totalBetween(from, to)
    }
}
