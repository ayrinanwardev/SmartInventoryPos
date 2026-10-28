package com.example.smartinventorypos.data.remote


data class ProductReq(
    val name: String, val barcode: String?, val price: Double,
    val stock: Int, val lowStockThreshold: Int = 5
)