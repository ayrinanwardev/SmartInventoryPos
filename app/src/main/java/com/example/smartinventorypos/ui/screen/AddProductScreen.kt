package com.example.smartinventorypos.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.smartinventorypos.ui.PosViewModel
import kotlinx.coroutines.launch

@Composable
fun AddProductScreen(vm: PosViewModel, onDone: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var barcode by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Add product", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(
            name,
            { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            barcode,
            { barcode = it },
            label = { Text("Barcode (optional)") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            price,
            { price = it },
            label = { Text("Price") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            stock,
            { stock = it },
            label = { Text("Stock") },
            modifier = Modifier.fillMaxWidth()
        )
        message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                scope.launch {
                    val err = vm.addProduct(name, barcode, price, stock)
                    if (err == null) {
                        message = "Saved: $name"
                        name = ""; barcode = ""; price = ""; stock = ""
                    } else message = err
                }
            }) { Text("Save") }
            OutlinedButton(onClick = onDone) { Text("Back") }
        }
    }

}