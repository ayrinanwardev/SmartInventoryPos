package smart_inventory_backend.concurrency

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import smart_inventory_backend.sale.SaleService
import smart_inventory_backend.stock.StockAdjustRequest
import smart_inventory_backend.stock.StockService

/** Manual stock changes made at the same time must all count, and each must be audited. */
class StockAdjustmentConcurrencyTest : ConcurrencyTestBase() {

    @Autowired private lateinit var stock: StockService
    @Autowired private lateinit var sales: SaleService

    @Test
    fun `simultaneous adjustments are all applied and each is audited in order`() {
        val sugar = product("Sugar 1kg", stock = 50)

        val results = runConcurrently(30) { i ->
            if (i % 2 == 0) stock.adjust(sugar, StockAdjustRequest(+2, "restock"), manager)
            else stock.adjust(sugar, StockAdjustRequest(-1, "damaged"), cashier)
        }

        assertNoFailures(results)
        assertEquals(50 + 15 * 2 - 15, stockOf(sugar))
        assertEquals(30, rowCount("stock_adjustments"))
        // The row lock serialises the changes, so replaying the audit log in order must reproduce
        // every recorded level exactly. A lost update would show up as a level that doesn't add up.
        val log = jdbc.query("select delta, stock_after from stock_adjustments order by id") { rs, _ ->
            rs.getInt("delta") to rs.getInt("stock_after")
        }
        var level = 50
        log.forEach { (delta, after) ->
            level += delta
            assertEquals(level, after)
        }
    }

    @Test
    fun `sales and adjustments at the same time keep stock exact`() {
        val lentils = product("Red Lentils 1kg", stock = 100)

        val results = runConcurrently(30) { i ->
            if (i < 20) sales.checkout(sale(lentils to 1), cashier)
            else stock.adjust(lentils, StockAdjustRequest(+3, "delivery"), manager)
        }

        assertNoFailures(results)
        assertEquals(100 - 20 + 10 * 3, stockOf(lentils))
    }
}
