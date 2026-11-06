package smart_inventory_backend.sale

import org.springframework.stereotype.Component
import smart_inventory_backend.auth.Role
import smart_inventory_backend.common.InvalidRequestException
import java.math.BigDecimal

class DiscountExceedsSubtotalException :
    InvalidRequestException("DISCOUNT_EXCEEDS_SUBTOTAL", "Discount exceeds subtotal")

/** Works out the discount a sale actually gets. New discount rules can be added as new implementations. */
fun interface DiscountPolicy {
    fun resolve(requested: BigDecimal?, subtotal: BigDecimal, role: Role): BigDecimal
}

/** Staff can give any discount up to the subtotal; customers can't discount their own purchases. */
@Component
class RoleBasedDiscountPolicy : DiscountPolicy {
    override fun resolve(requested: BigDecimal?, subtotal: BigDecimal, role: Role): BigDecimal {
        val discount = if (role == Role.CUSTOMER) BigDecimal.ZERO else (requested ?: BigDecimal.ZERO)
        if (discount > subtotal) throw DiscountExceedsSubtotalException()
        return discount
    }
}
