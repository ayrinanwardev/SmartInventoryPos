package smart_inventory_backend.model

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "stock_adjustments")
class StockAdjustment(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(optional = false)
    var product: Product? = null,

    var delta: Int = 0,
    var stockAfter: Int = 0,
    var reason: String = "",
    var performedBy: String = "",
    var role: String = "",
    var createdAt: Instant = Instant.now()
)
