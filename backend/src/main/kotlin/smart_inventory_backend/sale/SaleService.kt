package smart_inventory_backend.sale

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import smart_inventory_backend.auth.Actor
import smart_inventory_backend.auth.UnknownUserException
import smart_inventory_backend.auth.UserRepository
import smart_inventory_backend.common.pageableOf
import smart_inventory_backend.product.ProductNotFoundException
import smart_inventory_backend.product.ProductRepository

@Service
class SaleService(
    private val sales: SaleRepository,
    private val products: ProductRepository,
    private val users: UserRepository,
    private val discountPolicy: DiscountPolicy
) {
    /** Idempotent: resending a clientId returns the saved sale, so offline retries can't sell twice. */
    @Transactional
    fun checkout(req: SaleRequest, actor: Actor): SaleResponse {
        sales.findByClientId(req.clientId)?.let { return it.toResponse() }

        val buyer = users.findByUsername(actor.username) ?: throw UnknownUserException(actor.username)
        val sale = Sale(req.clientId, buyer)
        for (item in req.items) {
            val product = products.lockById(item.productId) ?: throw ProductNotFoundException(item.productId)
            sale.addItem(product, item.qty)
        }
        sale.applyDiscount(discountPolicy.resolve(req.discount, sale.subtotal, actor.role))
        return sales.saveAndFlush(sale).toResponse()
    }

    @Transactional(readOnly = true)
    fun history(page: Int?, size: Int?): List<SaleResponse> =
        sales.findAllByOrderByCreatedAtDesc(pageableOf(page, size)).map { it.toResponse() }

    @Transactional(readOnly = true)
    fun salesBy(username: String, page: Int?, size: Int?): List<SaleResponse> =
        sales.findByCashierUsernameOrderByCreatedAtDesc(username, pageableOf(page, size)).map { it.toResponse() }
}
