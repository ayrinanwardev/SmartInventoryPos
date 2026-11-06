package smart_inventory_backend.auth

import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<AppUser, Long> {
    fun findByUsername(username: String): AppUser?
}
