package smart_inventory_backend.product

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant

/** State only changes through the methods below, so the stock rules live in exactly one place. */
@Entity
@Table(name = "products")
class Product(
    name: String = "",
    barcode: String? = null,
    price: BigDecimal = BigDecimal.ZERO,
    stock: Int = 0,
    lowStockThreshold: Int = 5
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        private set

    @Column(nullable = false)
    var name: String = name
        private set

    @Column(unique = true)
    var barcode: String? = barcode
        private set

    var price: BigDecimal = price
        private set

    var stock: Int = stock
        private set

    var lowStockThreshold: Int = lowStockThreshold
        private set

    var active: Boolean = true
        private set

    var updatedAt: Instant = Instant.now()
        private set

    /** Takes [qty] out of stock for a sale. */
    fun sell(qty: Int) {
        if (!active) throw ProductUnavailableException(name)
        if (stock < qty) throw InsufficientStockException(name)
        stock -= qty
    }

    /** Applies a manual correction and returns the new stock level. */
    fun adjustStock(delta: Int): Int {
        val newStock = stock + delta
        if (newStock < 0) throw StockBelowZeroException(stock)
        stock = newStock
        return newStock
    }

    /** Adds stock at a new price. Also brings a deleted product back. */
    fun restock(qty: Int, newPrice: BigDecimal) {
        stock += qty
        price = newPrice
        active = true
    }

    fun edit(name: String, barcode: String?, price: BigDecimal, stock: Int, lowStockThreshold: Int) {
        this.name = name
        this.barcode = barcode
        this.price = price
        this.stock = stock
        this.lowStockThreshold = lowStockThreshold
    }

    /** Soft delete: past sales still need to reference this product. */
    fun deactivate() {
        active = false
    }

    @PrePersist
    @PreUpdate
    private fun touch() {
        updatedAt = Instant.now()
    }
}
