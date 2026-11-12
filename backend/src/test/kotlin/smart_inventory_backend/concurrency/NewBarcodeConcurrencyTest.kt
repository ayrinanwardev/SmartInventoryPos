package smart_inventory_backend.concurrency

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import smart_inventory_backend.product.ProductRequest
import smart_inventory_backend.product.ProductService
import java.math.BigDecimal

/**
 * A new product with a barcode is added from two devices at once (or the same delivery is booked
 * twice). There's no row to lock yet, so both requests try to create it.
 */
class NewBarcodeConcurrencyTest : ConcurrencyTestBase() {

    @Autowired private lateinit var products: ProductService

    @Test
    fun `the same new barcode added many times at once becomes one product with all the stock`() {
        val delivery = ProductRequest("Ghee 500g", "8941100500999", BigDecimal("650.00"), stock = 5)

        val results = runConcurrently(10) { products.createOrRestock(delivery) }

        assertNoFailures(results)
        assertEquals(1, rowCount("products"))
        assertEquals(50, jdbc.queryForObject("select stock from products where barcode = '8941100500999'", Int::class.java))
    }
}
