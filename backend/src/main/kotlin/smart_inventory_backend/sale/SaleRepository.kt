package smart_inventory_backend.sale

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

interface SaleRepository : JpaRepository<Sale, Long> {
    fun findByClientId(clientId: UUID): Sale?
    fun findAllByOrderByCreatedAtDesc(pageable: Pageable): List<Sale>
    fun findByCashierUsernameOrderByCreatedAtDesc(username: String, pageable: Pageable): List<Sale>

    @Query("select coalesce(sum(s.total), 0) from Sale s where s.createdAt >= :from and s.createdAt < :to")
    fun totalBetween(@Param("from") from: Instant, @Param("to") to: Instant): BigDecimal
}
