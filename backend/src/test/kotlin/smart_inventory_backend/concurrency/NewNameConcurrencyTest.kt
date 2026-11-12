package smart_inventory_backend.concurrency

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import smart_inventory_backend.product.NameInUseException
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

    @Test
    fun `renaming a product to a name in use is NAME_IN_USE, and a deleted twin is allowed`() {
        val sugar = product("Sugar 1kg", stock = 10)
        val oldSugar = product("Old Sugar", stock = 0)
        products.delete(oldSugar)

        assertThrows<NameInUseException> {
            products.update(product("Brown Sugar", stock = 5), ProductRequest("SUGAR 1KG", null, BigDecimal("140.00"), stock = 5))
        }
        // An inactive product may share the active one's name.
        products.update(oldSugar, ProductRequest("Sugar 1kg", null, BigDecimal("135.00"), stock = 0))
        assertEquals(1, jdbc.queryForObject("select count(*) from products where active and lower(name) = 'sugar 1kg'", Int::class.java))
        assertEquals(sugar, jdbc.queryForObject("select id from products where active and lower(name) = 'sugar 1kg'", Long::class.java))
    }
}
