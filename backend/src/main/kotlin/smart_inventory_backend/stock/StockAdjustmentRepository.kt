package smart_inventory_backend.stock

import org.springframework.data.jpa.repository.JpaRepository

interface StockAdjustmentRepository : JpaRepository<StockAdjustment, Long> {
    fun findTop50ByOrderByCreatedAtDesc(): List<StockAdjustment>
}
