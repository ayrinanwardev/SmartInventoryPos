package smart_inventory_backend.stock

import org.springframework.stereotype.Component
import smart_inventory_backend.auth.Role
import smart_inventory_backend.common.ForbiddenException
import smart_inventory_backend.common.InvalidRequestException
import kotlin.math.abs

class ZeroStockChangeException : InvalidRequestException("ZERO_STOCK_CHANGE", "Stock change cannot be 0")

class StockChangeLimitExceededException(limit: Int) :
    ForbiddenException("STOCK_CHANGE_LIMIT_EXCEEDED", "Cashiers can change at most $limit items at once")

/** Decides whether someone in a given role may make a stock change. */
fun interface StockAdjustmentPolicy {
    /** @throws smart_inventory_backend.common.DomainException if the change is not allowed */
    fun check(delta: Int, role: Role)
}

/** Managers are unlimited; cashiers may only make small corrections. */
@Component
class RoleBasedStockAdjustmentPolicy : StockAdjustmentPolicy {
    override fun check(delta: Int, role: Role) {
        if (delta == 0) throw ZeroStockChangeException()
        if (role == Role.CASHIER && abs(delta) > CASHIER_LIMIT) {
            throw StockChangeLimitExceededException(CASHIER_LIMIT)
        }
    }

    private companion object {
        const val CASHIER_LIMIT = 20
    }
}
