package com.example.smartinventorypos.ui.screen

import android.os.Build
import androidx.annotation.RequiresExtension
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartinventorypos.data.remote.ProductDto
import com.example.smartinventorypos.ui.PosViewModel
import kotlinx.coroutines.launch

@RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
@Composable
 fun ProductsTab(vm: PosViewModel) {
    val list by vm.adminProducts.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val error by vm.screenError.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<ProductDto?>(null) }
    var deleting by remember { mutableStateOf<ProductDto?>(null) }
    var actionError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { vm.loadAdminProducts() }

    Column(Modifier.padding(12.dp)) {
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (editing == null) {
            actionError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
        LazyColumn {
            items(list, key = { it.id }) { p ->
                ListItem(
                    headlineContent = { Text(p.name) },
                    supportingContent = {
                        Text("৳%.2f  •  stock %d  •  %s".format(p.price, p.stock, p.barcode ?: "no barcode"))
                    },
                    trailingContent = {
                        Row {
                            TextButton(onClick = { actionError = null; editing = p }) { Text("Edit") }
                            TextButton(onClick = { actionError = null; deleting = p }) { Text("Delete") }
                        }
                    }
                )
                HorizontalDivider()
            }
        }
    }

    editing?.let { p ->
        EditProductDialog(
            p = p,
            error = actionError,
            onDismiss = { editing = null },
            onSave = { name, barcode, price, stock, threshold ->
                scope.launch {
                    val err = vm.updateProduct(p.id, name, barcode, price, stock, threshold)
                    if (err == null) { actionError = null; editing = null } else actionError = err
                }
            }
        )
    }

    deleting?.let { p ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete product?") },
            text = { Text(p.name) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        actionError = vm.deleteProduct(p.id)
                        deleting = null
                    }
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun EditProductDialog(
    p: ProductDto,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(p.name) }
    var barcode by remember { mutableStateOf(p.barcode ?: "") }
    var price by remember { mutableStateOf(p.price.toString()) }
    var stock by remember { mutableStateOf(p.stock.toString()) }
    var threshold by remember { mutableStateOf(p.lowStockThreshold.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit product") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") })
                OutlinedTextField(barcode, { barcode = it }, label = { Text("Barcode") })
                OutlinedTextField(price, { price = it }, label = { Text("Price") })
                OutlinedTextField(stock, { stock = it }, label = { Text("Stock") })
                OutlinedTextField(threshold, { threshold = it }, label = { Text("Low-stock limit") })
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, barcode, price, stock, threshold) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}