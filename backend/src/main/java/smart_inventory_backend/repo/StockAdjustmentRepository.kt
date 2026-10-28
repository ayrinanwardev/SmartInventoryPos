package smart_inventory_backend.repo

import org.springframework.data.jpa.repository.JpaRepository
import smart_inventory_backend.model.StockAdjustment

interface StockAdjustmentRepository : JpaRepository<StockAdjustment, Long> {
    fun findTop50ByOrderByCreatedAtDesc(): List<StockAdjustment>
}
