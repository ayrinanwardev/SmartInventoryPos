package com.example.smartinventorypos.data.remote

data class SaleReq(val clientId: String, val items: List<ItemReq>, val discount: Double)
