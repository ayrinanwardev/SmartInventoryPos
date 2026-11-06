package com.example.smartinventorypos.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartinventorypos.R
import com.example.smartinventorypos.domain.model.Product
import com.example.smartinventorypos.ui.common.UiText
import com.example.smartinventorypos.ui.common.resolve
import com.example.smartinventorypos.ui.components.CircleIconButton
import com.example.smartinventorypos.ui.components.EmptyState
import com.example.smartinventorypos.ui.components.ErrorBanner
import com.example.smartinventorypos.ui.components.LoadingBar
import com.example.smartinventorypos.ui.components.Monogram
import com.example.smartinventorypos.ui.components.PagePadding
import com.example.smartinventorypos.ui.components.PosDialog
import com.example.smartinventorypos.ui.components.PosTextField
import com.example.smartinventorypos.ui.components.SoftCard
import com.example.smartinventorypos.ui.components.StockBadge
import com.example.smartinventorypos.ui.money
import com.example.smartinventorypos.ui.theme.Danger
import com.example.smartinventorypos.ui.theme.Muted
import com.example.smartinventorypos.ui.viewmodel.ManageProductsViewModel
import com.example.smartinventorypos.ui.viewmodel.ProductFormInput

@Composable
fun ProductsTab(vm: ManageProductsViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.load() }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = PagePadding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.loading) LoadingBar()
            state.error?.let { ErrorBanner(it.resolve()) }
            if (state.editing == null) state.actionError?.let { ErrorBanner(it.resolve()) }
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 320.dp),
            contentPadding = PaddingValues(start = PagePadding, end = PagePadding, top = 8.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.products.isEmpty() && !state.loading && state.error == null) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState(
                        Icons.Outlined.Inventory2,
                        stringResource(R.string.no_products_yet),
                        stringResource(R.string.products_empty_message)
                    )
                }
            }
            items(state.products, key = { it.id }) { p ->
                ProductRow(p, onEdit = { vm.startEditing(p) }, onDelete = { vm.startDeleting(p) })
            }
        }
    }

    state.editing?.let { p ->
        EditProductDialog(p = p, error = state.actionError, onDismiss = vm::cancelEditing, onSave = vm::saveEdit)
    }

    state.deleting?.let { p ->
        PosDialog(
            title = stringResource(R.string.delete_product_title),
            confirmText = stringResource(R.string.delete),
            destructive = true,
            onDismiss = vm::cancelDeleting,
            onConfirm = vm::confirmDelete
        ) {
            Text(stringResource(R.string.delete_product_message, p.name), color = Muted)
        }
    }
}

@Composable
private fun ProductRow(p: Product, onEdit: () -> Unit, onDelete: () -> Unit) {
    SoftCard {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Monogram(p.name, Modifier.size(52.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(p.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(money(p.price), style = MaterialTheme.typography.bodyMedium)
                Text(
                    stringResource(R.string.product_row, p.stock, p.barcode ?: stringResource(R.string.no_barcode)),
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                StockBadge(p.stock, p.lowStockThreshold, Modifier.padding(top = 4.dp))
            }
            CircleIconButton(Icons.Outlined.Edit, stringResource(R.string.edit_named, p.name), onEdit)
            CircleIconButton(
                Icons.Outlined.DeleteOutline,
                stringResource(R.string.delete_named, p.name),
                onDelete,
                tint = Danger
            )
        }
    }
}

@Composable
private fun EditProductDialog(
    p: Product,
    error: UiText?,
    onDismiss: () -> Unit,
    onSave: (ProductFormInput) -> Unit
) {
    var name by rememberSaveable(p.id) { mutableStateOf(p.name) }
    var barcode by rememberSaveable(p.id) { mutableStateOf(p.barcode ?: "") }
    var price by rememberSaveable(p.id) { mutableStateOf(p.price.toPlainString()) }
    var stock by rememberSaveable(p.id) { mutableStateOf(p.stock.toString()) }
    var threshold by rememberSaveable(p.id) { mutableStateOf(p.lowStockThreshold.toString()) }
    val number = KeyboardOptions(keyboardType = KeyboardType.Number)

    PosDialog(
        title = stringResource(R.string.edit_product),
        confirmText = stringResource(R.string.save),
        onDismiss = onDismiss,
        onConfirm = { onSave(ProductFormInput(name, barcode, price, stock, threshold)) }
    ) {
        PosTextField(name, { name = it }, stringResource(R.string.name), Modifier.fillMaxWidth())
        PosTextField(barcode, { barcode = it }, stringResource(R.string.barcode))
        PosTextField(
            price,
            { price = it },
            stringResource(R.string.price),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        PosTextField(stock, { stock = it }, stringResource(R.string.stock), keyboardOptions = number)
        PosTextField(threshold, { threshold = it }, stringResource(R.string.low_stock_limit), keyboardOptions = number)
        error?.let { ErrorBanner(it.resolve()) }
    }
}
