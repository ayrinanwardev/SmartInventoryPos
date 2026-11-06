package smart_inventory_backend.stock

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import smart_inventory_backend.auth.Actor
import smart_inventory_backend.product.ProductNotFoundException
import smart_inventory_backend.product.ProductRepository
import smart_inventory_backend.product.ProductResponse
import smart_inventory_backend.product.toResponse

@Service
class StockService(
    private val products: ProductRepository,
    private val adjustments: StockAdjustmentRepository,
    private val policy: StockAdjustmentPolicy
) {
    @Transactional
    fun adjust(productId: Long, req: StockAdjustRequest, actor: Actor): ProductResponse {
        policy.check(req.delta, actor.role)
        val product = products.lockById(productId) ?: throw ProductNotFoundException(productId)
        val stockAfter = product.adjustStock(req.delta)
        adjustments.save(
            StockAdjustment(product, req.delta, stockAfter, req.reason.trim(), actor.username, actor.role)
        )
        return products.saveAndFlush(product).toResponse()
    }

    @Transactional(readOnly = true)
    fun history(): List<StockAdjustmentResponse> =
        adjustments.findTop50ByOrderByCreatedAtDesc().map { it.toResponse() }
}
