package com.example.smartinventorypos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartinventorypos.R
import com.example.smartinventorypos.domain.common.AppResult
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

data class AddProductUiState(
    val form: ProductFormInput = ProductFormInput(),
    val busy: Boolean = false,
    val error: UiText? = null,
    val saved: UiText? = null
)

@HiltViewModel
class AddProductViewModel @Inject constructor(
    private val products: ProductAdminRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AddProductUiState())
    val state: StateFlow<AddProductUiState> = _state.asStateFlow()

    fun onFormChange(form: ProductFormInput) = _state.update { it.copy(form = form) }

    fun save() {
        val current = _state.value
        if (current.busy) return
        val draft = current.form.toDraft()
        if (draft == null) {
            _state.update { it.copy(error = UiText(R.string.error_product_form), saved = null) }
            return
        }
        _state.update { it.copy(busy = true, error = null, saved = null) }
        viewModelScope.launch {
            when (val result = products.add(draft)) {
                is AppResult.Success ->
                    _state.value = AddProductUiState(saved = UiText(R.string.product_saved, draft.name))
                is AppResult.Failure ->
                    _state.update { it.copy(busy = false, error = result.error.toUiText()) }
            }
        }
    }
}
