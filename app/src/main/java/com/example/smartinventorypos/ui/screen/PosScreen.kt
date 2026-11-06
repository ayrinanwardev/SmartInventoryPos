package com.example.smartinventorypos.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartinventorypos.R
import com.example.smartinventorypos.domain.model.Cart
import com.example.smartinventorypos.domain.model.CartLine
import com.example.smartinventorypos.domain.model.Money
import com.example.smartinventorypos.domain.model.Product
import com.example.smartinventorypos.domain.model.Role
import com.example.smartinventorypos.ui.common.UiText
import com.example.smartinventorypos.ui.common.sessionViewModel
import com.example.smartinventorypos.ui.components.CircleIconButton
import com.example.smartinventorypos.ui.components.EmptyState
import com.example.smartinventorypos.ui.components.Monogram
import com.example.smartinventorypos.ui.components.PagePadding
import com.example.smartinventorypos.ui.components.PosDialog
import com.example.smartinventorypos.ui.components.PosTextField
import com.example.smartinventorypos.ui.components.PrimaryButton
import com.example.smartinventorypos.ui.components.SearchField
import com.example.smartinventorypos.ui.components.ScreenHeader
import com.example.smartinventorypos.ui.components.SoftCard
import com.example.smartinventorypos.ui.components.StockBadge
import com.example.smartinventorypos.ui.money
import com.example.smartinventorypos.ui.theme.Brown
import com.example.smartinventorypos.ui.theme.Hairline
import com.example.smartinventorypos.ui.theme.Ink
import com.example.smartinventorypos.ui.theme.Mist
import com.example.smartinventorypos.ui.theme.Muted
import com.example.smartinventorypos.ui.theme.Yellow
import com.example.smartinventorypos.ui.viewmodel.PosViewModel

private val TwoPaneWidth = 600.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    modifier: Modifier = Modifier,
    onLogout: () -> Unit,
    onMessage: (UiText) -> Unit,
    vm: PosViewModel = sessionViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val discountText by vm.discountText.collectAsStateWithLifecycle()
    val cart = state.cart
    var cartOpen by rememberSaveable { mutableStateOf(false) }
    var confirmLogout by rememberSaveable { mutableStateOf(false) }

    val role = vm.role
    val isShopper = role?.hasOrderHistory == true

    LaunchedEffect(vm) { vm.messages.collect { onMessage(it) } }

    val cartPanel: @Composable (Modifier) -> Unit = { listModifier ->
        CartPanel(
            cart = cart,
            products = state.products,
            discount = discountText,
            showDiscount = role?.canGiveDiscount == true,
            total = state.total,
            checkoutLabel = stringResource(if (isShopper) R.string.buy_now else R.string.checkout),
            listModifier = listModifier,
            onChange = vm::changeQuantity,
            onDiscount = vm::onDiscountChange,
            onCheckout = vm::checkout
        )
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val twoPane = maxWidth >= TwoPaneWidth
        Row(Modifier.fillMaxSize()) {
            Column(Modifier.weight(1f).fillMaxHeight()) {
                ScreenHeader(
                    title = stringResource(if (isShopper) R.string.shop_title else R.string.pos_title),
                    subtitle = headerSubtitle(role, state.pendingSales)
                ) {
                    CircleIconButton(Icons.Outlined.Refresh, stringResource(R.string.refresh), vm::refresh)
                    CircleIconButton(
                        Icons.AutoMirrored.Outlined.Logout,
                        stringResource(R.string.sign_out),
                        { confirmLogout = true }
                    )
                }
                SearchField(
                    query,
                    vm::onQueryChange,
                    stringResource(R.string.search_products),
                    Modifier.padding(horizontal = PagePadding)
                )
                ProductGrid(
                    products = state.products,
                    cart = cart,
                    searching = query.isNotBlank(),
                    bottomSpace = if (!twoPane && !cart.isEmpty) 88.dp else 16.dp,
                    onAdd = vm::add,
                    modifier = Modifier.weight(1f)
                )
            }
            if (twoPane) {
                SoftCard(
                    Modifier
                        .width(340.dp)
                        .fillMaxHeight()
                        .padding(top = 12.dp, end = PagePadding, bottom = 12.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        cartPanel(Modifier.weight(1f))
                    }
                }
            }
        }

        if (!twoPane && !cart.isEmpty) {
            CartBar(
                count = cart.itemCount,
                total = state.total,
                onClick = { cartOpen = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = PagePadding, vertical = 12.dp)
            )
        }

        if (!twoPane && cartOpen && !cart.isEmpty) {
            ModalBottomSheet(
                onDismissRequest = { cartOpen = false },
                containerColor = Color.White,
                shape = MaterialTheme.shapes.large
            ) {
                Column(Modifier.padding(start = PagePadding, end = PagePadding, bottom = 24.dp)) {
                    cartPanel(Modifier.heightIn(max = 260.dp))
                }
            }
        }
    }

    if (confirmLogout) {
        PosDialog(
            title = stringResource(R.string.sign_out_title),
            confirmText = stringResource(R.string.sign_out),
            onConfirm = { confirmLogout = false; onLogout() },
            onDismiss = { confirmLogout = false }
        ) {
            Text(stringResource(R.string.sign_out_message), color = Muted)
        }
    }
}

@Composable
private fun headerSubtitle(role: Role?, pending: Int): String {
    val who = stringResource(
        when (role) {
            Role.MANAGER -> R.string.role_manager
            Role.CASHIER -> R.string.role_cashier
            Role.CUSTOMER -> R.string.role_customer
            null -> R.string.role_unknown
        }
    )
    return if (pending > 0) stringResource(R.string.pos_subtitle_pending, who, pending)
    else stringResource(R.string.pos_subtitle_synced, who)
}

@Composable
private fun ProductGrid(
    products: List<Product>,
    cart: Cart,
    searching: Boolean,
    bottomSpace: Dp,
    onAdd: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = PagePadding, end = PagePadding, top = 16.dp, bottom = bottomSpace),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        if (products.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyState(
                    Icons.Outlined.Inventory2,
                    stringResource(if (searching) R.string.no_matching_products else R.string.no_products_yet),
                    stringResource(if (searching) R.string.try_another_search else R.string.tap_refresh)
                )
            }
        }
        items(products, key = { it.id }) { p ->
            ProductCard(p, inCart = cart.quantityOf(p.id), onAdd = { onAdd(p) })
        }
    }
}

@Composable
private fun ProductCard(p: Product, inCart: Int, onAdd: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box {
            Monogram(p.name, Modifier.fillMaxWidth().aspectRatio(1.7f), background = Mist)
            StockBadge(p.stock, threshold = p.lowStockThreshold, modifier = Modifier.align(Alignment.TopStart).padding(8.dp))
        }
        Text(
            p.name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(money(p.price), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.in_stock_count, p.stock),
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted
                )
            }
            AddButton(inCart, enabled = p.stock > inCart, name = p.name, onClick = onAdd)
        }
    }
}

/** A card shows its in-cart count on this button once the product has been added. */
@Composable
private fun AddButton(inCart: Int, enabled: Boolean, name: String, onClick: () -> Unit) {
    if (inCart == 0) {
        CircleIconButton(
            Icons.Outlined.Add,
            stringResource(R.string.add_named, name),
            onClick,
            filled = enabled,
            enabled = enabled
        )
    } else {
        Surface(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            color = Brown,
            contentColor = Yellow
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("$inCart", style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Composable
private fun CartBar(count: Int, total: Money, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = CircleShape,
        color = Yellow,
        contentColor = Ink,
        shadowElevation = 6.dp
    ) {
        Row(
            Modifier.padding(horizontal = 20.dp).height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Outlined.ShoppingBag, contentDescription = null, Modifier.size(20.dp))
            Text(
                pluralStringResource(R.plurals.cart_items, count, count),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f)
            )
            Text(money(total), style = MaterialTheme.typography.titleMedium)
            Icon(
                Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = stringResource(R.string.open_cart),
                Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun CartPanel(
    cart: Cart,
    products: List<Product>,
    discount: String,
    showDiscount: Boolean,
    total: Money,
    checkoutLabel: String,
    listModifier: Modifier,
    onChange: (Long, Int) -> Unit,
    onDiscount: (String) -> Unit,
    onCheckout: () -> Unit
) {
    Text(stringResource(R.string.cart), style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(12.dp))
    if (cart.isEmpty) {
        Box(listModifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.error_empty_cart), style = MaterialTheme.typography.bodyMedium, color = Muted)
        }
    } else {
        LazyColumn(listModifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(cart.lines, key = { it.productId }) { line ->
                // The product can be hidden by the search filter; the server still checks stock at sync.
                val stock = products.find { it.id == line.productId }?.stock
                CartRow(line, canAddMore = stock == null || line.quantity < stock, onChange)
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    if (showDiscount) {
        PosTextField(
            discount,
            onDiscount,
            stringResource(R.string.discount),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        Spacer(Modifier.height(12.dp))
    }
    HorizontalDivider(color = Hairline)
    Spacer(Modifier.height(12.dp))
    TotalLine(stringResource(R.string.subtotal), money(cart.subtotal), emphasized = false)
    Spacer(Modifier.height(4.dp))
    TotalLine(stringResource(R.string.total), money(total), emphasized = true)
    Spacer(Modifier.height(16.dp))
    PrimaryButton(checkoutLabel, onCheckout, Modifier.fillMaxWidth(), enabled = !cart.isEmpty)
}

@Composable
private fun CartRow(line: CartLine, canAddMore: Boolean, onChange: (Long, Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f)) {
            Text(line.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(money(line.total), style = MaterialTheme.typography.bodySmall, color = Muted)
        }
        Row(
            Modifier.clip(CircleShape).background(Color.White),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CircleIconButton(
                Icons.Outlined.Remove,
                stringResource(R.string.remove_one_named, line.name),
                { onChange(line.productId, -1) }
            )
            Text("${line.quantity}", style = MaterialTheme.typography.titleSmall)
            CircleIconButton(
                Icons.Outlined.Add,
                stringResource(R.string.add_one_named, line.name),
                { onChange(line.productId, 1) },
                filled = canAddMore,
                enabled = canAddMore
            )
        }
    }
}

@Composable
private fun TotalLine(label: String, value: String, emphasized: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = if (emphasized) Ink else Muted,
            modifier = Modifier.weight(1f)
        )
        Text(value, style = if (emphasized) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium)
    }
}
