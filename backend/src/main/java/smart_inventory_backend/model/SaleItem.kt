package smart_inventory_backend.model
import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "sale_items")
class SaleItem(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @get:JsonIgnore   // avoids Sale -> items -> sale -> ... infinite JSON loop
    @ManyToOne(optional = false)
    var sale: Sale? = null,

    @ManyToOne(optional = false)
    var product: Product? = null,

    var qty: Int = 0,
    var unitPrice: BigDecimal = BigDecimal.ZERO
)
