package smart_inventory_backend.concurrency

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import smart_inventory_backend.sale.SaleService
import java.util.UUID

/**
 * The phone may resend a sale while the first upload is still being processed, for example after
 * a client timeout. Every copy must get the same saved sale back, and stock must drop only once.
 */
class DuplicateSaleConcurrencyTest : ConcurrencyTestBase() {

    @Autowired private lateinit var sales: SaleService

    @Test
    fun `the same sale sent many times at once is saved once and every copy gets it back`() {
        val tea = product("Black Tea 400g", stock = 50)
        val clientId = UUID.randomUUID()

        val results = runConcurrently(10) { sales.checkout(sale(tea to 3, clientId = clientId), cashier) }

        assertNoFailures(results)
        assertEquals(1, results.map { it.getOrThrow().id }.distinct().size)
        assertEquals(1, rowCount("sales"))
        assertEquals(47, stockOf(tea))
    }
}
