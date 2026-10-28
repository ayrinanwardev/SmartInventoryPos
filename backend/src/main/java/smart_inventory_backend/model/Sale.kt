package smart_inventory_backend.model
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.*


@Entity
@Table(name = "sales")
class Sale(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "client_id", unique = true, nullable = false)
    var clientId: UUID = UUID.randomUUID(),

    @ManyToOne
    var cashier: AppUser? = null,

    var subtotal: BigDecimal = BigDecimal.ZERO,
    var discount: BigDecimal = BigDecimal.ZERO,
    var total: BigDecimal = BigDecimal.ZERO,
    var createdAt: Instant = Instant.now(),

    @OneToMany(mappedBy = "sale", cascade = [CascadeType.ALL], orphanRemoval = true)
    var items: MutableList<SaleItem> = mutableListOf()
)