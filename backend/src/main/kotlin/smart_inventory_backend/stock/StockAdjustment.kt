package smart_inventory_backend.stock

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import smart_inventory_backend.auth.Role
import smart_inventory_backend.product.Product
import java.time.Instant

/** An audit record, so it never changes once written. */
@Entity
@Table(name = "stock_adjustments")
class StockAdjustment(
    product: Product? = null,
    delta: Int = 0,
    stockAfter: Int = 0,
    reason: String = "",
    performedBy: String = "",
    role: Role = Role.CASHIER
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        private set

    @ManyToOne(optional = false)
    var product: Product? = product
        private set

    var delta: Int = delta
        private set

    var stockAfter: Int = stockAfter
        private set

    var reason: String = reason
        private set

    var performedBy: String = performedBy
        private set

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    var role: Role = role
        private set

    var createdAt: Instant = Instant.now()
        private set
}
