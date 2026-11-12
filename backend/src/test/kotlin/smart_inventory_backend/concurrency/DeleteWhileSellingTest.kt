package smart_inventory_backend.concurrency

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import smart_inventory_backend.product.ProductService
import smart_inventory_backend.product.ProductUnavailableException
import smart_inventory_backend.sale.SaleService

/**
 * A manager removes a product while tills are still selling it. Deactivating must not write back a
 * stock value it read before those sales committed.
 */
class DeleteWhileSellingTest : ConcurrencyTestBase() {

    @Autowired private lateinit var products: ProductService
    @Autowired private lateinit var sales: SaleService

    @Test
    fun `deleting a product while it sells never overwrites the stock`() {
        repeat(15) { round ->
            val start = 1_000
            val milk = product("Dairy Milk $round", stock = start)

            val results = runConcurrently(21) { i ->
                if (i == 10) products.delete(milk) else sales.checkout(sale(milk to 1), cashier)
            }

            val sold = results.withIndex().count { (i, r) -> i != 10 && r.isSuccess }
            val unexpected = results.mapNotNull { it.exceptionOrNull() }.filterNot { it is ProductUnavailableException }
            assertEquals(emptyList<Throwable>(), unexpected) { "Only PRODUCT_UNAVAILABLE may fail (round $round)" }
            assertEquals(start - sold, stockOf(milk)) { "Stock lost a sale (round $round)" }
            assertFalse(jdbc.queryForObject("select active from products where id = ?", Boolean::class.java, milk)!!)
        }
    }
}
