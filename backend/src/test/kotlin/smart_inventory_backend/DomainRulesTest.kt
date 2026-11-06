package smart_inventory_backend

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import smart_inventory_backend.auth.Role
import smart_inventory_backend.product.InsufficientStockException
import smart_inventory_backend.product.Product
import smart_inventory_backend.product.ProductUnavailableException
import smart_inventory_backend.product.StockBelowZeroException
import smart_inventory_backend.sale.DiscountExceedsSubtotalException
import smart_inventory_backend.sale.RoleBasedDiscountPolicy
import smart_inventory_backend.sale.Sale
import smart_inventory_backend.stock.RoleBasedStockAdjustmentPolicy
import smart_inventory_backend.stock.StockChangeLimitExceededException
import smart_inventory_backend.stock.ZeroStockChangeException
import java.math.BigDecimal

/** Business rules, tested without Spring or a database. */
class DomainRulesTest {

    private fun product(stock: Int = 5, price: String = "10.00") =
        Product("Milk", null, BigDecimal(price), stock)

    // ----- Product -----
    @Test fun `selling reduces stock`() {
        val p = product(stock = 5)
        p.sell(2)
        assertEquals(3, p.stock)
    }

    @Test fun `cannot sell more than in stock`() {
        val p = product(stock = 1)
        assertThrows(InsufficientStockException::class.java) { p.sell(2) }
        assertEquals(1, p.stock)
    }

    @Test fun `cannot sell a deleted product`() {
        val p = product().apply { deactivate() }
        assertThrows(ProductUnavailableException::class.java) { p.sell(1) }
    }

    @Test fun `stock adjustment returns the new level and cannot go negative`() {
        val p = product(stock = 2)
        assertEquals(5, p.adjustStock(3))
        assertThrows(StockBelowZeroException::class.java) { p.adjustStock(-6) }
        assertEquals(5, p.stock)
    }

    @Test fun `restock adds stock, sets the price and brings back a deleted product`() {
        val p = product(stock = 2).apply { deactivate() }
        p.restock(3, BigDecimal("12.00"))
        assertEquals(5, p.stock)
        assertEquals(BigDecimal("12.00"), p.price)
        assertTrue(p.active)
    }

    @Test fun `deactivate hides the product`() {
        assertFalse(product().apply { deactivate() }.active)
    }

    // ----- Sale -----
    @Test fun `sale totals follow its items and discount`() {
        val sale = Sale()
        sale.addItem(product(price = "10.00"), 2)
        sale.addItem(product(price = "5.50"), 1)
        sale.applyDiscount(BigDecimal("1.50"))
        assertEquals(BigDecimal("25.50"), sale.subtotal)
        assertEquals(BigDecimal("24.00"), sale.total)
        assertEquals(2, sale.items.size)
    }

    @Test fun `a sale item keeps the price from the time of sale`() {
        val p = product(price = "10.00")
        val sale = Sale().apply { addItem(p, 1) }
        p.restock(0, BigDecimal("99.00"))
        assertEquals(BigDecimal("10.00"), sale.items.single().unitPrice)
    }

    // ----- Discount policy -----
    private val discounts = RoleBasedDiscountPolicy()

    @Test fun `staff discount applies`() =
        assertEquals(BigDecimal("3"), discounts.resolve(BigDecimal("3"), BigDecimal("10"), Role.CASHIER))

    @Test fun `no discount requested means zero`() =
        assertEquals(BigDecimal.ZERO, discounts.resolve(null, BigDecimal("10"), Role.MANAGER))

    @Test fun `customer discount is ignored, even above the subtotal`() =
        assertEquals(BigDecimal.ZERO, discounts.resolve(BigDecimal("99"), BigDecimal("10"), Role.CUSTOMER))

    @Test fun `staff discount cannot exceed the subtotal`() {
        assertThrows(DiscountExceedsSubtotalException::class.java) {
            discounts.resolve(BigDecimal("11"), BigDecimal("10"), Role.MANAGER)
        }
    }

    // ----- Stock adjustment policy -----
    private val stockPolicy = RoleBasedStockAdjustmentPolicy()

    @Test fun `zero change is rejected for everyone`() {
        assertThrows(ZeroStockChangeException::class.java) { stockPolicy.check(0, Role.MANAGER) }
    }

    @Test fun `cashier limit applies in both directions`() {
        stockPolicy.check(20, Role.CASHIER)
        stockPolicy.check(-20, Role.CASHIER)
        assertThrows(StockChangeLimitExceededException::class.java) { stockPolicy.check(21, Role.CASHIER) }
        assertThrows(StockChangeLimitExceededException::class.java) { stockPolicy.check(-21, Role.CASHIER) }
    }

    @Test fun `manager has no limit`() = stockPolicy.check(10_000, Role.MANAGER)
}
