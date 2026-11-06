package com.example.smartinventorypos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartinventorypos.R
import com.example.smartinventorypos.domain.common.AppResult
import com.example.smartinventorypos.domain.common.DomainError
import com.example.smartinventorypos.domain.model.Product
import com.example.smartinventorypos.domain.model.Role
import com.example.smartinventorypos.domain.repository.ProductAdminRepository
import com.example.smartinventorypos.domain.repository.SessionStore
import com.example.smartinventorypos.ui.common.UiText
import com.example.smartinventorypos.ui.common.toUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StockUiState(
    val products: List<Product> = emptyList(),
    val loading: Boolean = false,
    val error: UiText? = null,
    val onlyLowStock: Boolean = false,
    val adjusting: Product? = null,
    val adjustError: UiText? = null,
    val saving: Boolean = false,
    val info: UiText? = null
) {
    val visibleProducts: List<Product>
        get() = if (onlyLowStock) products.filter { it.isLowStock } else products
}

@HiltViewModel
class StockViewModel @Inject constructor(
    sessions: SessionStore,
    private val products: ProductAdminRepository
) : ViewModel() {

    val role: Role? = sessions.session.value?.role

    private val _state = MutableStateFlow(StockUiState())
    val state: StateFlow<StockUiState> = _state.asStateFlow()

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val result = products.fetchAll()) {
                is AppResult.Success -> _state.update { it.copy(products = result.value, loading = false) }
                is AppResult.Failure -> _state.update { it.copy(error = result.error.toUiText(), loading = false) }
            }
        }
    }

    fun showOnlyLowStock(onlyLow: Boolean) = _state.update { it.copy(onlyLowStock = onlyLow) }

    fun startAdjusting(product: Product) =
        _state.update { it.copy(adjusting = product, adjustError = null, info = null) }

    fun cancelAdjusting() = _state.update { it.copy(adjusting = null, adjustError = null) }

    fun saveAdjustment(quantityText: String, isAddition: Boolean, reason: String) {
        val current = _state.value
        val product = current.adjusting ?: return
        if (current.saving) return // A quick second tap must not apply the change twice.
        val quantity = quantityText.toIntOrNull()
        if (quantity == null || quantity == 0 || reason.isBlank()) {
            _state.update { it.copy(adjustError = UiText(R.string.error_adjust_form)) }
            return
        }
        _state.update { it.copy(saving = true, adjustError = null) }
        viewModelScope.launch {
            val delta = if (isAddition) quantity else -quantity
            when (val result = products.adjustStock(product.id, delta, reason.trim())) {
                is AppResult.Success -> {
                    _state.update {
                        it.copy(
                            adjusting = null,
                            saving = false,
                            info = UiText(R.string.stock_updated, product.name)
                        )
                    }
                    load()
                }
                is AppResult.Failure -> _state.update { it.copy(saving = false, adjustError = messageFor(result.error)) }
            }
        }
    }

    private fun messageFor(error: DomainError): UiText = when (error) {
        DomainError.Forbidden -> UiText(R.string.error_cashier_limit)
        DomainError.Conflict -> UiText(R.string.error_negative_stock)
        else -> error.toUiText()
    }
}
