package smart_inventory_backend.model
import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*


@Entity
@Table(name = "users")
class AppUser(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(unique = true, nullable = false)
    var username: String = "",

    @get:JsonIgnore   // never send the password hash in API responses
    @Column(name = "password_hash", nullable = false)
    var passwordHash: String? = "",

    @Column(nullable = false)
    var role: String = ""          // MANAGER | CASHIER
)