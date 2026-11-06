package com.example.smartinventorypos.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartinventorypos.R
import com.example.smartinventorypos.ui.common.resolve
import com.example.smartinventorypos.ui.components.EmptyState
import com.example.smartinventorypos.ui.components.ErrorBanner
import com.example.smartinventorypos.ui.components.LoadingBar
import com.example.smartinventorypos.ui.components.PagePadding
import com.example.smartinventorypos.ui.money
import com.example.smartinventorypos.ui.theme.Brown
import com.example.smartinventorypos.ui.theme.Yellow
import com.example.smartinventorypos.ui.viewmodel.SalesReportViewModel

@Composable
fun SalesTab(vm: SalesReportViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val noValue = stringResource(R.string.no_value)

    LaunchedEffect(Unit) { vm.load() }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = PagePadding, end = PagePadding, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(Brown)
                    .padding(20.dp)
            ) {
                Text(stringResource(R.string.today_sales), style = MaterialTheme.typography.labelMedium, color = Yellow)
                Text(
                    state.todayTotal?.let { money(it) } ?: noValue,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Yellow
                )
            }
        }
        if (state.loading) item { LoadingBar() }
        state.error?.let { error -> item { ErrorBanner(error.resolve()) } }
        if (state.sales.isEmpty() && !state.loading && state.error == null) {
            item {
                EmptyState(
                    Icons.AutoMirrored.Outlined.ReceiptLong,
                    stringResource(R.string.no_sales_title),
                    stringResource(R.string.no_sales_message)
                )
            }
        }
        items(state.sales, key = { it.id }) { s ->
            SaleCard(stringResource(R.string.sale_title, s.id), s, byline = s.cashierName ?: noValue)
        }
    }
}
