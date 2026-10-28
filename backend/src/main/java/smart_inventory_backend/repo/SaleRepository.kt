package smart_inventory_backend.repo

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import smart_inventory_backend.model.Sale
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID


interface SaleRepository : JpaRepository<Sale, Long> {
    fun findByClientId(clientId: UUID): Sale?
    fun findAllByOrderByCreatedAtDesc(): List<Sale>
    @Query("select coalesce(sum(s.total), 0) from Sale s where s.createdAt >= :from and s.createdAt < :to")
    fun totalBetween(@Param("from") from: Instant, @Param("to") to: Instant): BigDecimal
    fun findByCashierUsernameOrderByCreatedAtDesc(username: String): List<Sale>
}