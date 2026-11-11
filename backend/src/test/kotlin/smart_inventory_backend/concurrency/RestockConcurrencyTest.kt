package smart_inventory_backend.concurrency

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import smart_inventory_backend.product.ProductRequest
import smart_inventory_backend.product.ProductService
import smart_inventory_backend.sale.SaleService
import java.math.BigDecimal

/**
 * A delivery is booked (create-or-restock) while tills keep selling the same product.
 * Restock reads the product, adds to its stock and writes it back; without a row lock a sale
 * committed in between would be overwritten.
 */
class RestockConcurrencyTest : ConcurrencyTestBase() {

    @Autowired private lateinit var products: ProductService
    @Autowired private lateinit var sales: SaleService

    @Test
    fun `restocking while selling loses neither the delivery nor the sales`() {
        val rice = product("Basmati Rice 1kg", stock = 100, barcode = "8941100500050")
        val delivery = ProductRequest("Basmati Rice 1kg", "8941100500050", BigDecimal("80.00"), stock = 5)

        val results = runConcurrently(30) { i ->
            if (i < 20) sales.checkout(sale(rice to 1), cashier)
            else products.createOrRestock(delivery)
        }

        assertNoFailures(results)
        assertEquals(100 - 20 + 10 * 5, stockOf(rice))
    }

    @Test
    fun `restocking by name while selling keeps stock exact`() {
        val sugar = product("Sugar 1kg", stock = 60)
        val delivery = ProductRequest("sugar 1kg", null, BigDecimal("135.00"), stock = 4)

        val results = runConcurrently(24) { i ->
            if (i < 12) sales.checkout(sale(sugar to 2), cashier)
            else products.createOrRestock(delivery)
        }

        assertNoFailures(results)
        assertEquals(60 - 12 * 2 + 12 * 4, stockOf(sugar))
    }
}
