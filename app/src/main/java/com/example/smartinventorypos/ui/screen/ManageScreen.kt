package com.example.smartinventorypos.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.smartinventorypos.R
import com.example.smartinventorypos.ui.common.sessionViewModel
import com.example.smartinventorypos.ui.components.CircleIconButton
import com.example.smartinventorypos.ui.components.PagePadding
import com.example.smartinventorypos.ui.components.PillChip
import com.example.smartinventorypos.ui.components.ScreenHeader
import com.example.smartinventorypos.ui.viewmodel.ManageProductsViewModel
import com.example.smartinventorypos.ui.viewmodel.SalesReportViewModel

@Composable
fun ManageScreen(
    modifier: Modifier = Modifier,
    onAddProduct: () -> Unit,
    productsVm: ManageProductsViewModel = sessionViewModel(),
    salesVm: SalesReportViewModel = sessionViewModel()
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(modifier.fillMaxSize()) {
        ScreenHeader(stringResource(R.string.manage_title), subtitle = stringResource(R.string.manage_subtitle)) {
            CircleIconButton(
                Icons.Outlined.Refresh,
                stringResource(R.string.refresh),
                { if (tab == 0) productsVm.load() else salesVm.load() }
            )
            CircleIconButton(Icons.Outlined.Add, stringResource(R.string.add_product), onAddProduct, filled = true)
        }
        Row(
            Modifier.padding(start = PagePadding, end = PagePadding, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PillChip(stringResource(R.string.tab_products), tab == 0, { tab = 0 })
            PillChip(stringResource(R.string.tab_sales), tab == 1, { tab = 1 })
        }
        if (tab == 0) ProductsTab(productsVm) else SalesTab(salesVm)
    }
}
