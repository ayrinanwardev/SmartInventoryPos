package smart_inventory_backend.sale

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import smart_inventory_backend.auth.AppUser
import smart_inventory_backend.product.Product
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "sales")
class Sale(
    clientId: UUID = UUID.randomUUID(),
    cashier: AppUser? = null,
    createdAt: Instant = Instant.now()
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        private set

    @Column(name = "client_id", unique = true, nullable = false)
    var clientId: UUID = clientId
        private set

    /** Whoever placed the sale: a cashier or manager, or a customer buying online. */
    @ManyToOne
    var cashier: AppUser? = cashier
        private set

    var subtotal: BigDecimal = BigDecimal.ZERO
        private set

    var discount: BigDecimal = BigDecimal.ZERO
        private set

    var total: BigDecimal = BigDecimal.ZERO
        private set

    var createdAt: Instant = createdAt
        private set

    @OneToMany(mappedBy = "sale", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("id ASC")
    private var lines: MutableList<SaleItem> = mutableListOf()

    val items: List<SaleItem> get() = lines

    /** Takes the product out of stock at its current price and adds it to this sale. */
    fun addItem(product: Product, qty: Int) {
        product.sell(qty)
        val item = SaleItem(this, product, qty, product.price)
        lines.add(item)
        subtotal += item.lineTotal()
        total = subtotal - discount
    }

    fun applyDiscount(amount: BigDecimal) {
        discount = amount
        total = subtotal - amount
    }
}
