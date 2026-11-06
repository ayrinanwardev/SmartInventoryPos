package com.example.smartinventorypos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartinventorypos.domain.common.AppResult
import com.example.smartinventorypos.domain.model.Money
import com.example.smartinventorypos.domain.model.SaleRecord
import com.example.smartinventorypos.domain.repository.SaleHistoryRepository
import com.example.smartinventorypos.ui.common.UiText
import com.example.smartinventorypos.ui.common.toUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SalesReportUiState(
    val sales: List<SaleRecord> = emptyList(),
    val todayTotal: Money? = null,
    val loading: Boolean = false,
    val error: UiText? = null
)

@HiltViewModel
class SalesReportViewModel @Inject constructor(
    private val history: SaleHistoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SalesReportUiState())
    val state: StateFlow<SalesReportUiState> = _state.asStateFlow()

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val total = history.todayTotal()
            if (total is AppResult.Success) _state.update { it.copy(todayTotal = total.value) }
            val sales = history.allSales()
            if (sales is AppResult.Success) _state.update { it.copy(sales = sales.value) }
            val failure = listOf(total, sales).filterIsInstance<AppResult.Failure>().firstOrNull()
            _state.update { it.copy(loading = false, error = failure?.error?.toUiText()) }
        }
    }
}

data class MyOrdersUiState(
    val orders: List<SaleRecord> = emptyList(),
    val loading: Boolean = false,
    val error: UiText? = null
)

@HiltViewModel
class MyOrdersViewModel @Inject constructor(
    private val history: SaleHistoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MyOrdersUiState())
    val state: StateFlow<MyOrdersUiState> = _state.asStateFlow()

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val result = history.myOrders()) {
                is AppResult.Success -> _state.update { it.copy(orders = result.value, loading = false) }
                is AppResult.Failure -> _state.update { it.copy(error = result.error.toUiText(), loading = false) }
            }
        }
    }
}
