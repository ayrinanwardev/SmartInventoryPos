package smart_inventory_backend.concurrency

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import smart_inventory_backend.product.InsufficientStockException
import smart_inventory_backend.sale.SaleService

/** Many tills selling at the same moment must never sell stock that isn't there. */
class CheckoutConcurrencyTest : ConcurrencyTestBase() {

    @Autowired private lateinit var sales: SaleService

    @Test
    fun `only one of many cashiers can sell the last item`() {
        val rice = product("Basmati Rice 1kg", stock = 1)

        val results = runConcurrently(16) { sales.checkout(sale(rice to 1), cashier) }

        assertEquals(1, results.count { it.isSuccess })
        assertTrue(results.filter { it.isFailure }.all { it.exceptionOrNull() is InsufficientStockException }) {
            "Every losing sale must fail as out of stock, not with a database error"
        }
        assertEquals(0, stockOf(rice))
        assertEquals(1, rowCount("sales"))
    }

    @Test
    fun `parallel sales never oversell`() {
        val oil = product("Mustard Oil 1L", stock = 20)

        val results = runConcurrently(40) { sales.checkout(sale(oil to 1), cashier) }

        assertEquals(20, results.count { it.isSuccess })
        assertEquals(20, results.count { it.exceptionOrNull() is InsufficientStockException })
        assertEquals(0, stockOf(oil))
        assertEquals(20, jdbc.queryForObject("select sum(qty) from sale_items", Int::class.java))
    }

    @Test
    fun `parallel multi-item sales keep every product's stock exact`() {
        val rice = product("Basmati Rice 1kg", stock = 500)
        val tea = product("Black Tea 400g", stock = 500)

        val results = runConcurrently(20) { sales.checkout(sale(rice to 2, tea to 3), cashier) }

        assertNoFailures(results)
        assertEquals(500 - 20 * 2, stockOf(rice))
        assertEquals(500 - 20 * 3, stockOf(tea))
    }
}
