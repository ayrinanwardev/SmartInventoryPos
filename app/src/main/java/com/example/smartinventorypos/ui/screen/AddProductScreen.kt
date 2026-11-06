package com.example.smartinventorypos.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartinventorypos.R
import com.example.smartinventorypos.ui.common.resolve
import com.example.smartinventorypos.ui.common.sessionViewModel
import com.example.smartinventorypos.ui.components.ErrorBanner
import com.example.smartinventorypos.ui.components.InfoBanner
import com.example.smartinventorypos.ui.components.PagePadding
import com.example.smartinventorypos.ui.components.PosTextField
import com.example.smartinventorypos.ui.components.PrimaryButton
import com.example.smartinventorypos.ui.components.ScreenHeader
import com.example.smartinventorypos.ui.viewmodel.AddProductViewModel

@Composable
fun AddProductScreen(
    modifier: Modifier = Modifier,
    onDone: () -> Unit,
    vm: AddProductViewModel = sessionViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val form = state.form
    val next = KeyboardOptions(imeAction = ImeAction.Next)

    Column(modifier.fillMaxSize().imePadding()) {
        ScreenHeader(
            stringResource(R.string.add_product),
            subtitle = stringResource(R.string.add_product_subtitle),
            onBack = onDone
        )
        Box(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(horizontal = PagePadding, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PosTextField(
                    form.name,
                    { vm.onFormChange(form.copy(name = it)) },
                    stringResource(R.string.name),
                    keyboardOptions = next
                )
                PosTextField(
                    form.barcode,
                    { vm.onFormChange(form.copy(barcode = it)) },
                    stringResource(R.string.barcode_optional),
                    keyboardOptions = next
                )
                PosTextField(
                    form.price,
                    { vm.onFormChange(form.copy(price = it)) },
                    stringResource(R.string.price),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next)
                )
                PosTextField(
                    form.stock,
                    { vm.onFormChange(form.copy(stock = it)) },
                    stringResource(R.string.stock),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
                )
                state.error?.let { ErrorBanner(it.resolve()) }
                state.saved?.let { InfoBanner(it.resolve()) }
                PrimaryButton(
                    text = stringResource(R.string.save_product),
                    busy = state.busy,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
                    onClick = vm::save
                )
            }
        }
    }
}
