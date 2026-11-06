package smart_inventory_backend.product

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProductService(private val products: ProductRepository) {

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
     */
    @Transactional
    fun createOrRestock(req: ProductRequest): ProductResponse {
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
            throw BarcodeInUseException()
        }
    }

    @Transactional
    fun delete(id: Long) {
        val product = products.findByIdOrNull(id) ?: throw ProductNotFoundException(id)
        product.deactivate()
    }

    private fun findDuplicate(name: String, barcode: String?): Product? =
        if (barcode != null) products.findByBarcode(barcode) else products.findFirstByNameIgnoreCase(name)

    private fun normalizeBarcode(barcode: String?): String? = barcode?.trim()?.ifBlank { null }
}
