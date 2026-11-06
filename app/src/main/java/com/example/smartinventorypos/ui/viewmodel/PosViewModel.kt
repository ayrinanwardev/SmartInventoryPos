package com.example.smartinventorypos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartinventorypos.R
import com.example.smartinventorypos.domain.common.AppResult
import com.example.smartinventorypos.domain.common.DomainError
import com.example.smartinventorypos.domain.model.Cart
import com.example.smartinventorypos.domain.model.Money
import com.example.smartinventorypos.domain.model.Product
import com.example.smartinventorypos.domain.model.Role
import com.example.smartinventorypos.domain.repository.OfflineSaleRepository
import com.example.smartinventorypos.domain.repository.ProductCatalogRepository
import com.example.smartinventorypos.domain.repository.SessionStore
import com.example.smartinventorypos.domain.usecase.CheckoutUseCase
import com.example.smartinventorypos.domain.usecase.RefreshCatalogUseCase
import com.example.smartinventorypos.ui.common.UiText
import com.example.smartinventorypos.ui.common.toUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PosUiState(
    val products: List<Product> = emptyList(),
    val cart: Cart = Cart(),
    val pendingSales: Int = 0,
    val discount: Money = Money.ZERO
) {
    val total: Money get() = (cart.subtotal - discount).coerceAtLeastZero()
}

@HiltViewModel
class PosViewModel @Inject constructor(
    sessions: SessionStore,
    catalog: ProductCatalogRepository,
    sales: OfflineSaleRepository,
    private val checkoutSale: CheckoutUseCase,
    private val refreshCatalog: RefreshCatalogUseCase
) : ViewModel() {

    /** Fixed for this ViewModel's life, since each login gets its own instance. */
    val role: Role? = sessions.session.value?.role

    // Text fields read these directly: going through combine() below could make typing lag.
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _discountText = MutableStateFlow("")
    val discountText: StateFlow<String> = _discountText.asStateFlow()

    private val cart = MutableStateFlow(Cart())
    private var checkingOut = false

    private val _messages = MutableSharedFlow<UiText>(extraBufferCapacity = 4)
    val messages: SharedFlow<UiText> = _messages

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<PosUiState> = combine(
        _query.flatMapLatest { catalog.search(it) },
        cart,
        sales.pendingCount(),
        _discountText
    ) { products, cart, pending, discountText ->
        PosUiState(products, cart, pending, discountFrom(discountText) ?: Money.ZERO)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PosUiState())

    fun onQueryChange(value: String) { _query.value = value }
    fun onDiscountChange(value: String) { _discountText.value = value }
    fun add(product: Product) = cart.update { it.add(product) }
    fun changeQuantity(productId: Long, delta: Int) = cart.update { it.changeQuantity(productId, delta) }

    fun refresh() {
        viewModelScope.launch {
            val result = refreshCatalog()
            if (result is AppResult.Failure) _messages.emit(result.error.toUiText())
        }
    }

    fun checkout() {
        if (checkingOut) return // A quick second tap must not save the sale twice.
        checkingOut = true
        viewModelScope.launch {
            when (val result = submitSale()) {
                is AppResult.Success -> {
                    cart.value = Cart()
                    _discountText.value = ""
                    _messages.emit(UiText(R.string.sale_saved))
                }
                is AppResult.Failure -> _messages.emit(result.error.toUiText())
            }
            checkingOut = false
        }
    }

    private suspend fun submitSale(): AppResult<Unit> {
        val discount = discountFrom(_discountText.value)
            ?: return AppResult.Failure(DomainError.InvalidDiscount)
        return checkoutSale(cart.value, discount)
    }

    /** Blank means no discount. Returns null for text that isn't a number. */
    private fun discountFrom(text: String): Money? = when {
        role?.canGiveDiscount != true || text.isBlank() -> Money.ZERO
        else -> Money.parse(text)
    }
}
