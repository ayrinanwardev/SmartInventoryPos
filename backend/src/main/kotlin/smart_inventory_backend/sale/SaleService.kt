package smart_inventory_backend.sale

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import smart_inventory_backend.auth.Actor
import smart_inventory_backend.auth.UnknownUserException
import smart_inventory_backend.auth.UserRepository
import smart_inventory_backend.common.pageableOf
import smart_inventory_backend.product.Product
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
        val locked = lockInIdOrder(req.items.map { it.productId })
        val sale = Sale(req.clientId, buyer)
        for (item in req.items) {
            sale.addItem(locked.getValue(item.productId), item.qty)
        }
        sale.applyDiscount(discountPolicy.resolve(req.discount, sale.subtotal, actor.role))
        return sales.saveAndFlush(sale).toResponse()
    }

    /**
     * Locks every product of a sale in ascending id order. If each sale locked in the order its
     * items arrived, two sales listing the same products in opposite order could each hold one
     * lock and wait for the other: a deadlock. One global order makes that cycle impossible.
     */
    private fun lockInIdOrder(productIds: List<Long>): Map<Long, Product> =
        productIds.distinct().sorted().associateWith { id ->
            products.lockById(id) ?: throw ProductNotFoundException(id)
        }

    @Transactional(readOnly = true)
    fun history(page: Int?, size: Int?): List<SaleResponse> =
        sales.findAllByOrderByCreatedAtDesc(pageableOf(page, size)).map { it.toResponse() }

    @Transactional(readOnly = true)
    fun salesBy(username: String, page: Int?, size: Int?): List<SaleResponse> =
        sales.findByCashierUsernameOrderByCreatedAtDesc(username, pageableOf(page, size)).map { it.toResponse() }
}
