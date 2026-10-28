package smart_inventory_backend.repo

import org.springframework.data.jpa.repository.JpaRepository
import smart_inventory_backend.model.AppUser

interface UserRepository : JpaRepository<AppUser, Long> {
    fun findByUsername(username: String): AppUser?
}