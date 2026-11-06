package com.example.smartinventorypos.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Refresh
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
import com.example.smartinventorypos.domain.model.Role
import com.example.smartinventorypos.ui.common.UiText
import com.example.smartinventorypos.ui.common.resolve
import com.example.smartinventorypos.ui.common.sessionViewModel
import com.example.smartinventorypos.ui.components.CircleIconButton
import com.example.smartinventorypos.ui.components.EmptyState
import com.example.smartinventorypos.ui.components.ErrorBanner
import com.example.smartinventorypos.ui.components.InfoBanner
import com.example.smartinventorypos.ui.components.LoadingBar
import com.example.smartinventorypos.ui.components.Monogram
import com.example.smartinventorypos.ui.components.PagePadding
import com.example.smartinventorypos.ui.components.PillChip
import com.example.smartinventorypos.ui.components.PosDialog
import com.example.smartinventorypos.ui.components.PosTextField
import com.example.smartinventorypos.ui.components.ScreenHeader
import com.example.smartinventorypos.ui.components.SmallPillButton
import com.example.smartinventorypos.ui.components.SoftCard
import com.example.smartinventorypos.ui.components.StockBadge
import com.example.smartinventorypos.ui.theme.Muted
import com.example.smartinventorypos.ui.viewmodel.StockViewModel

/**
 * Stock screen for MANAGER and CASHIER: an All / Low stock filter and adjustments with a reason.
 * The cashier limit (max 20 per change) is enforced by the backend.
 */
@Composable
fun StockScreen(modifier: Modifier = Modifier, vm: StockViewModel = sessionViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val shown = state.visibleProducts
    val onlyLow = state.onlyLowStock

    LaunchedEffect(Unit) { vm.load() }

    Column(modifier.fillMaxSize()) {
        ScreenHeader(
            stringResource(R.string.stock_title),
            subtitle = stringResource(
                if (vm.role == Role.CASHIER) R.string.stock_subtitle_cashier else R.string.stock_subtitle_manager
            )
        ) {
            CircleIconButton(Icons.Outlined.Refresh, stringResource(R.string.refresh), vm::load)
        }
        Column(
            Modifier.padding(start = PagePadding, end = PagePadding, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillChip(stringResource(R.string.filter_all), !onlyLow, { vm.showOnlyLowStock(false) })
                PillChip(stringResource(R.string.low_stock), onlyLow, { vm.showOnlyLowStock(true) })
            }
            if (state.loading) LoadingBar()
            state.error?.let { ErrorBanner(it.resolve()) }
            state.info?.let { InfoBanner(it.resolve()) }
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 320.dp),
            contentPadding = PaddingValues(start = PagePadding, end = PagePadding, top = 4.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (shown.isEmpty() && !state.loading && state.error == null) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState(
                        Icons.Outlined.Inventory2,
                        stringResource(if (onlyLow) R.string.stock_good_title else R.string.no_products_yet),
                        stringResource(if (onlyLow) R.string.stock_good_message else R.string.stock_empty_message)
                    )
                }
            }
            items(shown, key = { it.id }) { p ->
                StockRow(p) { vm.startAdjusting(p) }
            }
        }
    }

    state.adjusting?.let { p ->
        AdjustDialog(
            p = p,
            error = state.adjustError,
            saving = state.saving,
            onDismiss = vm::cancelAdjusting,
            onSave = vm::saveAdjustment
        )
    }
}

@Composable
private fun StockRow(p: Product, onAdjust: () -> Unit) {
    SoftCard {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Monogram(p.name, Modifier.size(52.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(p.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    stringResource(R.string.stock_row, p.stock, p.lowStockThreshold),
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted
                )
                StockBadge(p.stock, p.lowStockThreshold, Modifier.padding(top = 4.dp))
            }
            SmallPillButton(stringResource(R.string.adjust), onAdjust)
        }
    }
}

@Composable
private fun AdjustDialog(
    p: Product,
    error: UiText?,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (quantity: String, isAddition: Boolean, reason: String) -> Unit
) {
    var add by rememberSaveable(p.id) { mutableStateOf(true) }
    var qty by rememberSaveable(p.id) { mutableStateOf("") }
    var reason by rememberSaveable(p.id) { mutableStateOf("") }

    PosDialog(
        title = p.name,
        confirmText = stringResource(R.string.save),
        busy = saving,
        onDismiss = onDismiss,
        onConfirm = { onSave(qty, add, reason) }
    ) {
        Text(stringResource(R.string.current_stock, p.stock), style = MaterialTheme.typography.bodyMedium, color = Muted)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillChip(stringResource(R.string.add), add, { add = true })
            PillChip(stringResource(R.string.remove), !add, { add = false })
        }
        PosTextField(
            qty,
            { qty = it.filter { c -> c.isDigit() } },
            stringResource(R.string.quantity),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        PosTextField(reason, { reason = it }, stringResource(R.string.adjust_reason))
        error?.let { ErrorBanner(it.resolve()) }
    }
}
