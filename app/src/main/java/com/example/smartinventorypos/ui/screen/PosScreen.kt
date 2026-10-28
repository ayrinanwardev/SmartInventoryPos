package com.example.smartinventorypos.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartinventorypos.ui.PosViewModel
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(vm: PosViewModel, onSuccess: () -> Unit) {
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("Sign in", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(user, { user = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(pass, { pass = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())
        if (error) Text("Login failed", color = MaterialTheme.colorScheme.error)
        Button(onClick = { scope.launch { if (vm.login(user, pass)) onSuccess() else error = true } },
            modifier = Modifier.padding(top = 12.dp)) { Text("Login") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(vm: PosViewModel) {
    val products by vm.products.collectAsStateWithLifecycle()
    val cart by vm.cart.collectAsStateWithLifecycle()
    val pending by vm.pending.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val discount by vm.discount.collectAsStateWithLifecycle()
    val snack = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { vm.messages.collect { snack.showSnackbar(it) } }

    val subtotal = cart.sumOf { it.price * it.qty }
    val total = subtotal - (discount.toDoubleOrNull() ?: 0.0)

    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        topBar = {
            TopAppBar(title = { Text("POS") }, actions = {
                Text("Pending sync: $pending")
                TextButton(onClick = vm::refresh) { Text("Refresh") }
            })
        }
    ) { pad ->
        Column(Modifier.padding(pad).padding(12.dp)) {
            OutlinedTextField(query, { vm.query.value = it }, label = { Text("Search name / barcode") },
                modifier = Modifier.fillMaxWidth())
            LazyColumn(Modifier.weight(1f)) {
                items(products, key = { it.id }) { p ->
                    ListItem(
                        headlineContent = { Text(p.name) },
                        supportingContent = { Text("৳%.2f  •  stock ${p.stock}".format(p.price)) },
                        trailingContent = { Button(onClick = { vm.add(p) }, enabled = p.stock > 0) { Text("Add") } }
                    )
                    HorizontalDivider()
                }
            }
            Text("Cart", style = MaterialTheme.typography.titleMedium)
            LazyColumn(Modifier.heightIn(max = 160.dp)) {
                items(cart, key = { it.productId }) { l ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(l.name, Modifier.weight(1f))
                        TextButton({ vm.change(l.productId, -1) }) { Text("−") }
                        Text("${l.qty}")
                        TextButton({ vm.change(l.productId, +1) }) { Text("+") }
                        Text("%.2f".format(l.price * l.qty))
                    }
                }
            }
            OutlinedTextField(discount, { vm.discount.value = it }, label = { Text("Discount") },
                modifier = Modifier.fillMaxWidth())
            Text("Subtotal %.2f   Total %.2f".format(subtotal, total), style = MaterialTheme.typography.titleMedium)
            Button(onClick = vm::checkout, enabled = cart.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                Text("Checkout")
            }
        }
    }
}
