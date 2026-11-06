package smart_inventory_backend.auth

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@Table(name = "users")
class AppUser(
    username: String = "",
    passwordHash: String = "",
    role: Role = Role.CUSTOMER
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        private set

    @Column(unique = true, nullable = false)
    var username: String = username
        private set

    @Column(name = "password_hash", nullable = false)
    var passwordHash: String = passwordHash
        private set

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false)
    var role: Role = role
        private set
}
