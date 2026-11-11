package smart_inventory_backend.concurrency

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import smart_inventory_backend.sale.SaleService

/**
 * Two sales that list the same products in opposite order. If checkout locked rows in request
 * order, each could hold one lock and wait for the other, and PostgreSQL would abort one of them.
 */
class CheckoutDeadlockTest : ConcurrencyTestBase() {

    @Autowired private lateinit var sales: SaleService

    @Test
    fun `sales listing the same products in opposite order never deadlock`() {
        val rice = product("Basmati Rice 1kg", stock = 10_000)
        val oil = product("Mustard Oil 1L", stock = 10_000)
        val rounds = 25
        val threads = 8

        val results = (1..rounds).flatMap {
            runConcurrently(threads) { i ->
                if (i % 2 == 0) sales.checkout(sale(rice to 1, oil to 1), cashier)
                else sales.checkout(sale(oil to 1, rice to 1), cashier)
            }
        }

        assertNoFailures(results)
        assertEquals(10_000 - rounds * threads, stockOf(rice))
        assertEquals(10_000 - rounds * threads, stockOf(oil))
    }
}
