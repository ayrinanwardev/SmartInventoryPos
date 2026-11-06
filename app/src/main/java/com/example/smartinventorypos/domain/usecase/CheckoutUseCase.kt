package com.example.smartinventorypos.domain.usecase

import com.example.smartinventorypos.domain.common.AppResult
import com.example.smartinventorypos.domain.common.DomainError
import com.example.smartinventorypos.domain.model.Cart
import com.example.smartinventorypos.domain.model.Money
import com.example.smartinventorypos.domain.repository.OfflineSaleRepository
import com.example.smartinventorypos.domain.repository.SaleSyncScheduler
import com.example.smartinventorypos.domain.repository.SessionStore
import javax.inject.Inject

/** Saves a sale on the device, which works offline, then asks for it to be sent to the server. */
class CheckoutUseCase @Inject constructor(
    private val sessions: SessionStore,
    private val sales: OfflineSaleRepository,
    private val syncScheduler: SaleSyncScheduler
) {
    suspend operator fun invoke(cart: Cart, requestedDiscount: Money): AppResult<Unit> {
        if (cart.isEmpty) return AppResult.Failure(DomainError.EmptyCart)

        val mayDiscount = sessions.session.value?.role?.canGiveDiscount == true
        val discount = if (mayDiscount) requestedDiscount else Money.ZERO
        if (discount < Money.ZERO || discount > cart.subtotal) {
            return AppResult.Failure(DomainError.InvalidDiscount)
        }

        val result = sales.record(cart, discount)
        if (result is AppResult.Success) syncScheduler.schedule()
        return result
    }
}
