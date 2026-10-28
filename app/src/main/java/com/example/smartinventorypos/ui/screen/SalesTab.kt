package com.example.smartinventorypos.ui.screen

import android.os.Build
import androidx.annotation.RequiresExtension
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartinventorypos.ui.PosViewModel

@RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
@Composable
fun SalesTab(vm: PosViewModel) {
    val sales by vm.sales.collectAsStateWithLifecycle()
    val total by vm.todayTotal.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val error by vm.screenError.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.loadSales() }

    Column(Modifier.padding(12.dp)) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Today's sales", style = MaterialTheme.typography.labelMedium)
                Text(
                    total?.let { "৳%.2f".format(it) } ?: "-",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
        TextButton(onClick = vm::loadSales) { Text("Refresh") }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        LazyColumn {
            items(sales, key = { it.id }) { s ->
                Column(Modifier.padding(vertical = 8.dp)) {
                    Text("#${s.id}   ৳%.2f".format(s.total), style = MaterialTheme.typography.titleSmall)
                    Text(
                        "${s.cashier?.username ?: "-"}  •  ${s.createdAt.take(16).replace('T', ' ')} (UTC)",
                        style = MaterialTheme.typography.labelSmall
                    )
                    s.items.forEach { line ->
                        Text(
                            "• ${line.product?.name ?: "?"} × ${line.qty}  @ %.2f".format(line.unitPrice),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (s.discount > 0) {
                        Text("Discount ৳%.2f".format(s.discount), style = MaterialTheme.typography.bodySmall)
                    }
                }
                HorizontalDivider()
            }
        }
    }
}