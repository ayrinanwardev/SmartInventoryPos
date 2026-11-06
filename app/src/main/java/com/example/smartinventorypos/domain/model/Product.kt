package com.example.smartinventorypos.domain.model

data class Product(
    val id: Long,
    val name: String,
    val barcode: String?,
    val price: Money,
    val stock: Int,
    val lowStockThreshold: Int = DEFAULT_LOW_STOCK_THRESHOLD
) {
    val isLowStock: Boolean get() = stock <= lowStockThreshold

    companion object {
        /** Same default as the server. Also used for local products, whose table has no limit column. */
        const val DEFAULT_LOW_STOCK_THRESHOLD = 5
    }
}

/** The fields needed to create or update a product. */
data class ProductDraft(
    val name: String,
    val barcode: String?,
    val price: Money,
    val stock: Int,
    val lowStockThreshold: Int = Product.DEFAULT_LOW_STOCK_THRESHOLD
)
