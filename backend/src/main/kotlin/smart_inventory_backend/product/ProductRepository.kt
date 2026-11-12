package smart_inventory_backend.product

import jakarta.persistence.LockModeType
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ProductRepository : JpaRepository<Product, Long> {
    fun findByNameContainingIgnoreCaseOrBarcode(name: String, barcode: String): List<Product>

    @Query("select p from Product p where p.stock <= p.lowStockThreshold order by p.stock")
    fun findLowStock(): List<Product>

    /** Row lock so two cashiers can't sell the last item at the same time. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    fun lockById(@Param("id") id: Long): Product?

    /** Finds a product by barcode and locks its row, for read-modify-write such as restocking. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.barcode = :barcode")
    fun lockByBarcode(@Param("barcode") barcode: String): Product?

    /**
     * Finds products by name, ignoring case, and locks their rows. The active one comes first:
     * restocking an inactive twin would reactivate it next to the active one.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where lower(p.name) = lower(:name) order by p.active desc, p.id")
    fun lockByName(@Param("name") name: String, pageable: Pageable): List<Product>
}
