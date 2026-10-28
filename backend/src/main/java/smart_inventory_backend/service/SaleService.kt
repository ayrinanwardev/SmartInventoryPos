package smart_inventory_backend.service


import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import smart_inventory_backend.model.Sale
import smart_inventory_backend.model.SaleItem
import smart_inventory_backend.repo.ProductRepository
import smart_inventory_backend.repo.SaleRepository
import smart_inventory_backend.repo.UserRepository
import java.math.BigDecimal

@Service
class SaleService(
    private val sales: SaleRepository,
    private val products: ProductRepository,
    private val users: UserRepository
) {

    /**
     * Idempotent: same clientId returns the already-saved sale.
     * role: MANAGER / CASHIER (can give discount) or CUSTOMER (online buy, no discount).
     */
    @Transactional
    fun checkout(req: SaleReq, username: String, role: String): Sale {
        sales.findByClientId(req.clientId)?.let { return it }

        val buyer = users.findByUsername(username)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user")
        val sale = Sale(clientId = req.clientId, cashier = buyer)   // "cashier" = who placed the sale

        var subtotal = BigDecimal.ZERO
        for (item in req.items) {
            val product = products.lockById(item.productId)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Product ${item.productId} not found")
            if (product.stock < item.qty) {
                throw ResponseStatusException(HttpStatus.CONFLICT, "Insufficient stock: ${product.name}")
            }
            product.stock -= item.qty
            sale.items.add(SaleItem(sale = sale, product = product, qty = item.qty, unitPrice = product.price))
            subtotal += product.price * BigDecimal.valueOf(item.qty.toLong())
        }

        val discount = if (role == "CUSTOMER") BigDecimal.ZERO else (req.discount ?: BigDecimal.ZERO)
        if (discount > subtotal) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Discount exceeds subtotal")
        }
        sale.subtotal = subtotal
        sale.discount = discount
        sale.total = subtotal - discount
        return sales.save(sale)
    }
}