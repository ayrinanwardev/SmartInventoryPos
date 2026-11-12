package smart_inventory_backend.product

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate

@Service
class ProductService(
    private val products: ProductRepository,
    transactionManager: PlatformTransactionManager
) {
    private val writeTx = TransactionTemplate(transactionManager)

    @Transactional(readOnly = true)
    fun list(query: String?): List<ProductResponse> {
        val found = if (query.isNullOrBlank()) {
            products.findAll()
        } else {
            products.findByNameContainingIgnoreCaseOrBarcode(query, query)
        }
        return found.filter { it.active }.map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun lowStock(): List<ProductResponse> =
        products.findLowStock().filter { it.active }.map { it.toResponse() }

    /**
     * Restocks the product if it already exists (matched by barcode, or by name when there is no
     * barcode), otherwise creates it. The app's Add Product screen relies on this behaviour.
     *
     * When the product is new there's no row to lock, so two requests for it can both decide to
     * create it. The unique key lets only one insert through; the other's transaction is rolled
     * back, and it tries once more in a new one, where it finds the product and restocks it.
     */
    fun createOrRestock(req: ProductRequest): ProductResponse =
        try {
            writeTx.execute { createOrRestockOnce(req) }!!
        } catch (e: DataIntegrityViolationException) {
            writeTx.execute { createOrRestockOnce(req) }!!
        }

    private fun createOrRestockOnce(req: ProductRequest): ProductResponse {
        val barcode = normalizeBarcode(req.barcode)
        val name = req.name.trim()
        val existing = findDuplicate(name, barcode)
        val saved = if (existing != null) {
            existing.restock(req.stock, req.price)
            products.saveAndFlush(existing)
        } else {
            products.saveAndFlush(Product(name, barcode, req.price, req.stock, req.lowStockThreshold))
        }
        return saved.toResponse()
    }

    @Transactional
    fun update(id: Long, req: ProductRequest): ProductResponse {
        val product = products.findByIdOrNull(id) ?: throw ProductNotFoundException(id)
        product.edit(req.name, normalizeBarcode(req.barcode), req.price, req.stock, req.lowStockThreshold)
        try {
            return products.saveAndFlush(product).toResponse()
        } catch (e: DataIntegrityViolationException) {
            val constraint = e.mostSpecificCause.message.orEmpty()
            throw if (ACTIVE_NAME_INDEX in constraint) NameInUseException() else BarcodeInUseException()
        }
    }

    /**
     * Soft delete. The row is locked first: Hibernate writes the whole row back, so deactivating
     * a copy read before a concurrent sale committed would put that sale's stock back.
     */
    @Transactional
    fun delete(id: Long) {
        val product = products.lockById(id) ?: throw ProductNotFoundException(id)
        product.deactivate()
    }

    /**
     * Finds the product to restock and locks its row. Restock adds to the stock it read, so without
     * the lock a sale committed in between would be overwritten (a lost update).
     */
    private fun findDuplicate(name: String, barcode: String?): Product? =
        if (barcode != null) products.lockByBarcode(barcode)
        else products.lockByName(name, PageRequest.of(0, 1)).firstOrNull()

    private fun normalizeBarcode(barcode: String?): String? = barcode?.trim()?.ifBlank { null }

    private companion object {
        /** The unique index on active product names (db/vendor/postgresql/V2). */
        const val ACTIVE_NAME_INDEX = "ux_products_active_name"
    }
}
