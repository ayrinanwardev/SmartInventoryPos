package smart_inventory_backend.service

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import smart_inventory_backend.model.Product
import smart_inventory_backend.model.StockAdjustment
import smart_inventory_backend.repo.ProductRepository
import smart_inventory_backend.repo.StockAdjustmentRepository
import kotlin.math.abs

@Service
class StockService(
    private val products: ProductRepository,
    private val log: StockAdjustmentRepository
) {
    companion object { const val CASHIER_LIMIT = 20 }

    @Transactional
    fun adjust(productId: Long, req: StockAdjustReq, username: String, role: String): Product {
        if (req.delta == 0) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Change 0 hote pare na")
        }
        // Manager: unlimited. Cashier: small corrections only.
        if (role == "CASHIER" && abs(req.delta) > CASHIER_LIMIT) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Cashier ekbar e max $CASHIER_LIMIT ta change korte pare")
        }
        val product = products.lockById(productId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Product nei")

        val newStock = product.stock + req.delta
        if (newStock < 0) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Stock 0 er niche jabe na (ekhon ${product.stock})")
        }
        product.stock = newStock
        log.save(
            StockAdjustment(
                product = product, delta = req.delta, stockAfter = newStock,
                reason = req.reason.trim(), performedBy = username, role = role
            )
        )
        return products.save(product)
    }
}
