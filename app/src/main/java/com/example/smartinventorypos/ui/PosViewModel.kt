package com.example.smartinventorypos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartinventorypos.data.local.model.ProductEntity
import com.example.smartinventorypos.data.repo.CartLine
import com.example.smartinventorypos.data.repo.PosRepository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PosViewModel(private val repo: PosRepository) : ViewModel() {
    val query = MutableStateFlow("")
    val discount = MutableStateFlow("")
    val messages = MutableSharedFlow<String>(extraBufferCapacity = 4)

    @OptIn(ExperimentalCoroutinesApi::class)
    val products = query.flatMapLatest { repo.search(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val pending = repo.pendingCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _cart = MutableStateFlow<List<CartLine>>(emptyList())
    val cart: StateFlow<List<CartLine>> = _cart

    init { refresh() }
    fun refresh() { viewModelScope.launch { repo.refreshProducts() } }

    fun add(p: ProductEntity) = _cart.update { c ->
        val ex = c.find { it.productId == p.id }
        when {
            ex == null && p.stock > 0 -> c + CartLine(p.id, p.name, p.price, 1)
            ex != null && ex.qty < p.stock -> c.map { if (it.productId == p.id) it.copy(qty = it.qty + 1) else it }
            else -> c
        }
    }

    fun change(id: Long, delta: Int) = _cart.update { c ->
        c.mapNotNull { if (it.productId == id) it.copy(qty = it.qty + delta).takeIf { x -> x.qty > 0 } else it }
    }

    fun checkout() {
        viewModelScope.launch {
            val lines = _cart.value
            if (lines.isEmpty()) return@launch
            val sub = lines.sumOf { it.price * it.qty }
            val d = discount.value.toDoubleOrNull() ?: 0.0
            if (d < 0 || d > sub) { messages.emit("Invalid discount"); return@launch }
            repo.checkout(lines, d)
            _cart.value = emptyList(); discount.value = ""
            messages.emit("Sale saved")
        }
    }

    suspend fun login(u: String, p: String): Boolean =
        runCatching { repo.login(u, p) }.isSuccess
}
