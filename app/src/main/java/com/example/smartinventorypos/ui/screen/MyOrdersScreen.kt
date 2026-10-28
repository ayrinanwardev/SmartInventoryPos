package com.example.smartinventorypos.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartinventorypos.ui.PosViewModel


/** Customer order history. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyOrdersScreen(vm: PosViewModel, onBack: () -> Unit) {
    val orders by vm.myOrders.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val error by vm.screenError.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.loadMyOrders() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My orders") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
                actions = { TextButton(onClick = vm::loadMyOrders) { Text("Refresh") } }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).padding(12.dp)) {
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (!loading && error == null && orders.isEmpty()) Text("Ekhono kono order nei")
            LazyColumn {
                items(orders, key = { it.id }) { s ->
                    Column(Modifier.padding(vertical = 8.dp)) {
                        Text("Order #${s.id}   ৳%.2f".format(s.total), style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${s.createdAt.take(16).replace('T', ' ')} (UTC)",
                            style = MaterialTheme.typography.labelSmall
                        )
                        s.items.forEach { line ->
                            Text(
                                "• ${line.product?.name ?: "?"} × ${line.qty}  @ %.2f".format(line.unitPrice),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}