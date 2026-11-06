package com.example.smartinventorypos.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.smartinventorypos.R
import com.example.smartinventorypos.domain.model.Role

enum class Destination {
    SELL, STOCK, MANAGE, ADD_PRODUCT, ORDERS;

    fun isAllowedFor(role: Role?): Boolean = when (this) {
        SELL -> true
        STOCK -> role?.canAdjustStock == true
        MANAGE, ADD_PRODUCT -> role?.canManageProducts == true
        ORDERS -> role?.hasOrderHistory == true
    }

    /** Where the back button goes from here. */
    val parent: Destination
        get() = if (this == ADD_PRODUCT) MANAGE else SELL
}

data class BottomDestination(
    val destination: Destination,
    @param:StringRes val label: Int,
    val icon: ImageVector
)

fun bottomDestinations(role: Role?): List<BottomDestination> {
    if (role == null) return emptyList()
    val sellLabel = if (role.hasOrderHistory) R.string.nav_shop else R.string.nav_sell
    return listOf(
        BottomDestination(Destination.SELL, sellLabel, Icons.Outlined.Storefront),
        BottomDestination(Destination.STOCK, R.string.nav_stock, Icons.Outlined.Inventory2),
        BottomDestination(Destination.MANAGE, R.string.nav_manage, Icons.Outlined.Dashboard),
        BottomDestination(Destination.ORDERS, R.string.nav_orders, Icons.AutoMirrored.Outlined.ReceiptLong)
    ).filter { it.destination.isAllowedFor(role) }
}
