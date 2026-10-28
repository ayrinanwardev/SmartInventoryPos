package smart_inventory_backend.repo

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import smart_inventory_backend.model.Product

interface ProductRepository : JpaRepository<Product, Long> {
    fun findByNameContainingIgnoreCaseOrBarcode(name: String, barcode: String): List<Product>

    @Query("select p from Product p where p.stock <= p.lowStockThreshold order by p.stock")
    fun findLowStock(): List<Product>

    /** Row lock so two cashiers can't sell the last item at the same time. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    fun lockById(@Param("id") id: Long): Product?


    fun findByBarcode(barcode: String): Product?
    fun findFirstByNameIgnoreCase(name: String): Product?
}