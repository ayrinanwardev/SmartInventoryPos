package com.example.smartinventorypos.domain.model

data class CartLine(val productId: Long, val name: String, val unitPrice: Money, val quantity: Int) {
    val total: Money get() = unitPrice * quantity
}

/** An immutable cart. Each change returns a new cart, which suits StateFlow. */
data class Cart(val lines: List<CartLine> = emptyList()) {
    val isEmpty: Boolean get() = lines.isEmpty()
    val itemCount: Int get() = lines.sumOf { it.quantity }
    val subtotal: Money get() = lines.map { it.total }.sum()

    fun quantityOf(productId: Long): Int = lines.find { it.productId == productId }?.quantity ?: 0

    /** Adds one unit, unless that would take the cart past the product's stock. */
    fun add(product: Product): Cart {
        val inCart = quantityOf(product.id)
        return when {
            inCart >= product.stock -> this
            inCart == 0 -> copy(lines = lines + CartLine(product.id, product.name, product.price, 1))
            else -> changeQuantity(product.id, 1)
        }
    }

    /** Changes a line's quantity and removes the line when it reaches zero. */
    fun changeQuantity(productId: Long, delta: Int): Cart = copy(
        lines = lines.mapNotNull { line ->
            if (line.productId != productId) line
            else line.copy(quantity = line.quantity + delta).takeIf { it.quantity > 0 }
        }
    )
}
