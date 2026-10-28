package com.example.smartinventorypos.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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


/**
 * Stock screen for MANAGER and CASHIER.
 * - All / Low stock filter
 * - Adjust (+ / -) with a reason
 * - Cashier limit (max 20 per change) is enforced by the backend
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockScreen(vm: PosViewModel, role: String?, onBack: () -> Unit) {
    val list by vm.adminProducts.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val error by vm.screenError.collectAsStateWithLifecycle()

    var onlyLow by remember { mutableStateOf(false) }
    var adjusting by remember { mutableStateOf<ProductDto?>(null) }
    var actionError by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val shown = if (onlyLow) list.filter { it.stock <= it.lowStockThreshold } else list

    LaunchedEffect(Unit) { vm.loadAdminProducts() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stock") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
                actions = { TextButton(onClick = vm::loadAdminProducts) { Text("Refresh") } }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).padding(12.dp)) {
            if (role == "CASHIER") {
                Text(
                    "Cashier ekbar e max 20 ta change korte pare",
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !onlyLow, onClick = { onlyLow = false }, label = { Text("All") })
                FilterChip(selected = onlyLow, onClick = { onlyLow = true }, label = { Text("Low stock") })
            }

            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            info?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            if (onlyLow && !loading && error == null && shown.isEmpty()) {
                Text("Shob product er stock thik ache")
            }

            LazyColumn {
                items(shown, key = { it.id }) { p ->
                    val low = p.stock <= p.lowStockThreshold
                    ListItem(
                        headlineContent = { Text(p.name) },
                        supportingContent = {
                            Text(
                                "Stock ${p.stock}  (limit ${p.lowStockThreshold})" +
                                        if (p.stock == 0) "  •  OUT" else if (low) "  •  LOW" else ""
                            )
                        },
                        trailingContent = {
                            TextButton(onClick = {
                                actionError = null
                                info = null
                                adjusting = p
                            }) { Text("Adjust") }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    adjusting?.let { p ->
        AdjustDialog(
            p = p,
            error = actionError,
            onDismiss = { adjusting = null },
            onSave = { delta, reason ->
                scope.launch {
                    val err = vm.adjustStock(p.id, delta, reason)
                    if (err == null) {
                        info = "${p.name}: stock update hoyeche"
                        actionError = null
                        adjusting = null
                    } else {
                        actionError = err
                    }
                }
            }
        )
    }
}

@Composable
private fun AdjustDialog(
    p: ProductDto,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (delta: String, reason: String) -> Unit
) {
    var add by remember { mutableStateOf(true) }
    var qty by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(p.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Ekhon stock: ${p.stock}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = add, onClick = { add = true }, label = { Text("Add (+)") })
                    FilterChip(selected = !add, onClick = { add = false }, label = { Text("Remove (−)") })
                }
                OutlinedTextField(
                    qty, { qty = it.filter { c -> c.isDigit() } },
                    label = { Text("Quantity") }, singleLine = true
                )
                OutlinedTextField(
                    reason, { reason = it },
                    label = { Text("Karon (restock / damaged / count)") }
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val q = qty.toIntOrNull() ?: 0
                onSave((if (add) q else -q).toString(), reason)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
