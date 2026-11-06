package com.example.smartinventorypos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartinventorypos.R
import com.example.smartinventorypos.domain.common.AppResult
import com.example.smartinventorypos.domain.model.Money
import com.example.smartinventorypos.domain.model.Product
import com.example.smartinventorypos.domain.model.ProductDraft
import com.example.smartinventorypos.domain.repository.ProductAdminRepository
import com.example.smartinventorypos.ui.common.UiText
import com.example.smartinventorypos.ui.common.toUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The text of a product form. [toDraft] returns null if any field isn't valid. */
data class ProductFormInput(
    val name: String = "",
    val barcode: String = "",
    val price: String = "",
    val stock: String = "",
    val lowStockLimit: String = Product.DEFAULT_LOW_STOCK_THRESHOLD.toString()
) {
    fun toDraft(): ProductDraft? {
        val parsedPrice = Money.parse(price) ?: return null
        val parsedStock = stock.toIntOrNull() ?: return null
        val parsedLimit = lowStockLimit.toIntOrNull() ?: return null
        if (name.isBlank()) return null
        return ProductDraft(name.trim(), barcode.trim().ifBlank { null }, parsedPrice, parsedStock, parsedLimit)
    }
}

data class ManageProductsUiState(
    val products: List<Product> = emptyList(),
    val loading: Boolean = false,
    val error: UiText? = null,
    val editing: Product? = null,
    val deleting: Product? = null,
    val actionError: UiText? = null
)

@HiltViewModel
class ManageProductsViewModel @Inject constructor(
    private val products: ProductAdminRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ManageProductsUiState())
    val state: StateFlow<ManageProductsUiState> = _state.asStateFlow()

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val result = products.fetchAll()) {
                is AppResult.Success -> _state.update { it.copy(products = result.value, loading = false) }
                is AppResult.Failure -> _state.update { it.copy(error = result.error.toUiText(), loading = false) }
            }
        }
    }

    fun startEditing(product: Product) = _state.update { it.copy(editing = product, actionError = null) }
    fun cancelEditing() = _state.update { it.copy(editing = null) }
    fun startDeleting(product: Product) = _state.update { it.copy(deleting = product, actionError = null) }
    fun cancelDeleting() = _state.update { it.copy(deleting = null) }

    fun saveEdit(input: ProductFormInput) {
        val product = _state.value.editing ?: return
        val draft = input.toDraft()
        if (draft == null) {
            _state.update { it.copy(actionError = UiText(R.string.error_product_form_with_limit)) }
            return
        }
        viewModelScope.launch {
            when (val result = products.update(product.id, draft)) {
                is AppResult.Success -> {
                    _state.update { it.copy(editing = null, actionError = null) }
                    load()
                }
                is AppResult.Failure -> _state.update { it.copy(actionError = result.error.toUiText()) }
            }
        }
    }

    fun confirmDelete() {
        val product = _state.value.deleting ?: return
        viewModelScope.launch {
            when (val result = products.delete(product.id)) {
                is AppResult.Success -> {
                    _state.update { it.copy(deleting = null, actionError = null) }
                    load()
                }
                is AppResult.Failure ->
                    _state.update { it.copy(deleting = null, actionError = result.error.toUiText()) }
            }
        }
    }
}
