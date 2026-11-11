package smart_inventory_backend.sale

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import smart_inventory_backend.auth.Actor
import smart_inventory_backend.auth.UnknownUserException
import smart_inventory_backend.auth.UserRepository
import smart_inventory_backend.common.pageableOf
import smart_inventory_backend.product.Product
import smart_inventory_backend.product.ProductNotFoundException
import smart_inventory_backend.product.ProductRepository
import java.time.Clock
import java.time.Instant

@Service
class SaleService(
    private val sales: SaleRepository,
    private val products: ProductRepository,
    private val users: UserRepository,
    private val discountPolicy: DiscountPolicy,
    private val clock: Clock,
    transactionManager: PlatformTransactionManager
) {
    private val writeTx = TransactionTemplate(transactionManager)
    private val readTx = TransactionTemplate(transactionManager).apply { isReadOnly = true }

    /**
     * Idempotent: resending a clientId returns the saved sale, so offline retries can't sell twice.
     *
     * Two copies of one sale can arrive at the same moment (the app retries after a timeout while
     * the first request is still running). Both miss the lookup, and the unique key on client_id
     * lets only one insert succeed. The loser's transaction is already rolled back by then, so it
     * reads the winner's sale in a fresh transaction and returns that instead of an error.
     */
    fun checkout(req: SaleRequest, actor: Actor): SaleResponse =
        try {
            writeTx.execute { record(req, actor) }!!
        } catch (e: DataIntegrityViolationException) {
            readTx.execute { sales.findByClientId(req.clientId)?.toResponse() } ?: throw e
        }

    private fun record(req: SaleRequest, actor: Actor): SaleResponse {
        sales.findByClientId(req.clientId)?.let { return it.toResponse() }

        val buyer = users.findByUsername(actor.username) ?: throw UnknownUserException(actor.username)
        val locked = lockInIdOrder(req.items.map { it.productId })
        val sale = Sale(req.clientId, buyer, Instant.now(clock))
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
