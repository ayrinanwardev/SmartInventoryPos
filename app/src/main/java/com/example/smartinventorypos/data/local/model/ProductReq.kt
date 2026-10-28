package com.example.smartinventorypos.data.local.model

data class ProductReq(
    val name: String, val barcode: String?, val price: Double,
    val stock: Int, val lowStockThreshold: Int = 5
)
