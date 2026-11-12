package smart_inventory_backend.concurrency

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import smart_inventory_backend.product.ProductRequest
import smart_inventory_backend.product.ProductService
import java.math.BigDecimal

/**
 * Products without a barcode are matched by name. The same new name added at the same moment must
 * not end up as two products that each hold part of the stock.
 */
class NewNameConcurrencyTest : ConcurrencyTestBase() {

    @Autowired private lateinit var products: ProductService

    @Test
    fun `the same new name added many times at once becomes one product with all the stock`() {
        val results = runConcurrently(10) { i ->
            // Different capitalisation on purpose: names match regardless of case.
            val name = if (i % 2 == 0) "Puffed Rice 500g" else "puffed rice 500g"
            products.createOrRestock(ProductRequest(name, null, BigDecimal("60.00"), stock = 5))
        }

        assertNoFailures(results)
        assertEquals(1, rowCount("products"))
        assertEquals(50, jdbc.queryForObject("select stock from products", Int::class.java))
    }
}
