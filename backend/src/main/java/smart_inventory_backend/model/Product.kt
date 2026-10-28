package smart_inventory_backend.model
import jakarta.persistence.*
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal
import java.time.Instant


@Entity
@Table(name = "products")
class Product(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @field:NotBlank
    var name: String = "",

    @Column(unique = true)
    var barcode: String? = null,

    @field:DecimalMin("0.0")
    var price: BigDecimal = BigDecimal.ZERO,

    @field:Min(0)
    var stock: Int = 0,

    var lowStockThreshold: Int = 5,

    var active: Boolean = true,


    var updatedAt: Instant = Instant.now()
) {
    @PrePersist
    @PreUpdate
    fun touch() { updatedAt = Instant.now()
    }
}
