package com.example.smartinventorypos.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartinventorypos.R
import com.example.smartinventorypos.ui.common.resolve
import com.example.smartinventorypos.ui.common.sessionViewModel
import com.example.smartinventorypos.ui.components.CircleIconButton
import com.example.smartinventorypos.ui.components.EmptyState
import com.example.smartinventorypos.ui.components.ErrorBanner
import com.example.smartinventorypos.ui.components.LoadingBar
import com.example.smartinventorypos.ui.components.PagePadding
import com.example.smartinventorypos.ui.components.ScreenHeader
import com.example.smartinventorypos.ui.viewmodel.MyOrdersViewModel

/** Customer order history. */
@Composable
fun MyOrdersScreen(modifier: Modifier = Modifier, vm: MyOrdersViewModel = sessionViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.load() }

    Column(modifier.fillMaxSize()) {
        ScreenHeader(stringResource(R.string.orders_title), subtitle = stringResource(R.string.orders_subtitle)) {
            CircleIconButton(Icons.Outlined.Refresh, stringResource(R.string.refresh), vm::load)
        }
        LazyColumn(
            contentPadding = PaddingValues(start = PagePadding, end = PagePadding, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.loading) item { LoadingBar() }
            state.error?.let { error -> item { ErrorBanner(error.resolve()) } }
            if (state.orders.isEmpty() && !state.loading && state.error == null) {
                item {
                    EmptyState(
                        Icons.AutoMirrored.Outlined.ReceiptLong,
                        stringResource(R.string.no_orders_title),
                        stringResource(R.string.no_orders_message)
                    )
                }
            }
            items(state.orders, key = { it.id }) { SaleCard(stringResource(R.string.order_title, it.id), it) }
        }
    }
}
