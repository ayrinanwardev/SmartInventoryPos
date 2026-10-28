package com.example.smartinventorypos.data.remote

data class ProductDto(val id: Long, val name: String, val barcode: String?, val price: Double, val stock: Int)