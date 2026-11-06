package com.example.smartinventorypos.domain.model

/** What each kind of user may do. The backend enforces the same rules; these only shape the app. */
enum class Role(
    val canGiveDiscount: Boolean,
    val canAdjustStock: Boolean,
    val canManageProducts: Boolean,
    val hasOrderHistory: Boolean
) {
    MANAGER(canGiveDiscount = true, canAdjustStock = true, canManageProducts = true, hasOrderHistory = false),
    CASHIER(canGiveDiscount = true, canAdjustStock = true, canManageProducts = false, hasOrderHistory = false),
    CUSTOMER(canGiveDiscount = false, canAdjustStock = false, canManageProducts = false, hasOrderHistory = true);

    companion object {
        fun fromName(name: String?): Role? = entries.find { it.name == name }
    }
}
