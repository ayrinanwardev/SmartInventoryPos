package com.example.smartinventorypos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartinventorypos.data.local.model.ProductEntity
import com.example.smartinventorypos.data.repo.CartLine
import com.example.smartinventorypos.data.repo.PosRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import retrofit2.HttpException

class PosViewModel(private val repo: PosRepository) : ViewModel() {

    // ---------- POS / shop state ----------
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

    // ---------- online screens state (stock, manager, orders) ----------
    val adminProducts = MutableStateFlow<List<ProductDto>>(emptyList())
    val sales = MutableStateFlow<List<SaleHistoryDto>>(emptyList())
    val myOrders = MutableStateFlow<List<SaleHistoryDto>>(emptyList())
    val todayTotal = MutableStateFlow<Double?>(null)
    val loading = MutableStateFlow(false)
    val screenError = MutableStateFlow<String?>(null)

    init { refresh() }

    fun refresh() { viewModelScope.launch { repo.refreshProducts() } }

    // ---------- cart ----------
    fun add(p: ProductEntity) = _cart.update { c ->
        val ex = c.find { it.productId == p.id }
        when {
            ex == null && p.stock > 0 -> c + CartLine(p.id, p.name, p.price, 1)
            ex != null && ex.qty < p.stock ->
                c.map { if (it.productId == p.id) it.copy(qty = it.qty + 1) else it }
            else -> c
        }
    }

    fun change(id: Long, delta: Int) = _cart.update { c ->
        c.mapNotNull {
            if (it.productId == id) it.copy(qty = it.qty + delta).takeIf { x -> x.qty > 0 } else it
        }
    }

    fun clearCart() {
        _cart.value = emptyList()
        discount.value = ""
    }

    fun checkout() {
        viewModelScope.launch {
            val lines = _cart.value
            if (lines.isEmpty()) return@launch
            val sub = lines.sumOf { it.price * it.qty }
            // customers cannot give themselves a discount
            val d = if (Session.role == "CUSTOMER") 0.0 else (discount.value.toDoubleOrNull() ?: 0.0)
            if (d !in 0.0..sub) { messages.emit("Invalid discount"); return@launch }
            repo.checkout(lines, d)
            clearCart()
            messages.emit("Sale saved")
        }
    }

    // ---------- auth ----------
    suspend fun login(u: String, p: String): String? = try {
        repo.login(u, p); null
    } catch (e: HttpException) {
        if (e.code() == 401) "Username ba password bhul" else e.readable()
    } catch (e: Exception) { e.readable() }

    suspend fun register(u: String, p: String): String? = try {
        repo.register(u, p); null
    } catch (e: HttpException) {
        when (e.code()) {
            409 -> "Ei username newa hoyeche"
            400 -> "Username min 3, password min 6 character"
            else -> e.readable()
        }
    } catch (e: Exception) { e.readable() }

    // ---------- loaders ----------
    private fun load(block: suspend () -> Unit) {
        viewModelScope.launch {
            loading.value = true
            screenError.value = null
            try { block() } catch (e: Exception) { screenError.value = e.readable() }
            loading.value = false
        }
    }

    fun loadAdminProducts() = load { adminProducts.value = repo.adminProducts() }
    fun loadMyOrders() = load { myOrders.value = repo.myOrders() }
    fun loadSales() = load {
        todayTotal.value = repo.todayTotal()
        sales.value = repo.salesHistory()
    }

    // ---------- product management (manager) ----------
    suspend fun addProduct(name: String, barcode: String, price: String, stock: String): String? {
        val pr = price.toDoubleOrNull()
        val st = stock.toIntOrNull()
        if (name.isBlank() || pr == null || st == null) return "Name, price ar stock thikmoto dao"
        return try {
            repo.addProduct(name.trim(), barcode.trim(), pr, st)
            null
        } catch (e: Exception) { e.readable() }
    }

    suspend fun updateProduct(
        id: Long, name: String, barcode: String, price: String, stock: String, threshold: String
    ): String? {
        val pr = price.toDoubleOrNull()
        val st = stock.toIntOrNull()
        val th = threshold.toIntOrNull()
        if (name.isBlank() || pr == null || st == null || th == null) {
            return "Name, price, stock, limit thikmoto dao"
        }
        return try {
            repo.updateProduct(id, name.trim(), barcode.trim(), pr, st, th)
            loadAdminProducts()
            null
        } catch (e: Exception) { e.readable() }
    }

    suspend fun deleteProduct(id: Long): String? = try {
        repo.deleteProduct(id)
        loadAdminProducts()
        null
    } catch (e: Exception) { e.readable() }

    // ---------- stock update (manager + cashier) ----------
    suspend fun adjustStock(id: Long, delta: String, reason: String): String? {
        val d = delta.toIntOrNull()
        if (d == null || d == 0 || reason.isBlank()) return "Quantity ar karon dao"
        return try {
            repo.adjustStock(id, d, reason.trim())
            loadAdminProducts()
            null
        } catch (e: HttpException) {
            when (e.code()) {
                403 -> "Cashier max 20 ta change korte pare"
                409 -> "Stock 0 er niche jabe na"
                else -> e.readable()
            }
        } catch (e: Exception) { e.readable() }
    }
}

private fun Exception.readable(): String = when (this) {
    is HttpException -> when (code()) {
        401 -> "Session sesh, abar login koro"
        403 -> "Permission nei"
        409 -> "Conflict: barcode duplicate ba product bikri hoyeche"
        else -> "Server error ${code()}"
    }
    is IOException -> "Server e connect hocche na"
    else -> "${javaClass.simpleName}: $message"
}