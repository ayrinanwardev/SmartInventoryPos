package com.example.smartinventorypos.data.local.model

import androidx.room3.Embedded
import androidx.room3.Relation


data class SaleWithItems(
    @Embedded val sale: SaleEntity,
    @Relation(parentColumns = ["clientId"], entityColumns = ["saleClientId"])
    val items: List<SaleItemEntity>
)
