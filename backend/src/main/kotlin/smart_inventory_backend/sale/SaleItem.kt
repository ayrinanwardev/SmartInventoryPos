package smart_inventory_backend.sale

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import smart_inventory_backend.product.Product
import java.math.BigDecimal

@Entity
@Table(name = "sale_items")
class SaleItem(
    sale: Sale? = null,
    product: Product? = null,
    qty: Int = 0,
    unitPrice: BigDecimal = BigDecimal.ZERO
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        private set

    @ManyToOne(optional = false)
    var sale: Sale? = sale
        private set

    @ManyToOne(optional = false)
    var product: Product? = product
        private set

    var qty: Int = qty
        private set

    /** The price at the time of sale, which stays fixed if the product's price changes later. */
    var unitPrice: BigDecimal = unitPrice
        private set

    fun lineTotal(): BigDecimal = unitPrice * BigDecimal.valueOf(qty.toLong())
}
