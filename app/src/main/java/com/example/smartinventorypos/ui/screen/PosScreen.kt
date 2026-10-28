package com.example.smartinventorypos.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartinventorypos.ui.PosViewModel
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(vm: PosViewModel, onSuccess: () -> Unit) {
    var register by remember { mutableStateOf(false) }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
    ) {
        Text(
            if (register) "Create account" else "Sign in",
            style = MaterialTheme.typography.headlineMedium
        )
        OutlinedTextField(user, { user = it }, label = { Text("Username") }, singleLine = true,
            modifier = Modifier.fillMaxWidth())
        OutlinedTextField(pass, { pass = it }, label = { Text("Password") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        if (register) Text("Password minimum 6 character", style = MaterialTheme.typography.labelSmall)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Button(
            enabled = !busy,
            onClick = {
                scope.launch {
                    busy = true
                    error = null
                    val err = if (register) vm.register(user.trim(), pass) else vm.login(user.trim(), pass)
                    busy = false
                    if (err == null) onSuccess() else error = err
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (register) "Create account" else "Login") }

        TextButton(onClick = { register = !register; error = null }) {
            Text(if (register) "Already have an account? Login" else "New customer? Create account")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    vm: PosViewModel,
    role: String?,
    onAddProduct: () -> Unit,
    onManage: () -> Unit,
    onStock: () -> Unit,
    onOrders: () -> Unit,
    onLogout: () -> Unit
) {
    val products by vm.products.collectAsStateWithLifecycle()
    val cart by vm.cart.collectAsStateWithLifecycle()
    val pending by vm.pending.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val discount by vm.discount.collectAsStateWithLifecycle()
    val snack = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { vm.messages.collect { snack.showSnackbar(it) } }

    val isCustomer = role == "CUSTOMER"
    val isManager = role == "MANAGER"
    val subtotal = cart.sumOf { it.price * it.qty }
    val discountValue = if (isCustomer) 0.0 else (discount.toDoubleOrNull() ?: 0.0)
    val total = subtotal - discountValue

    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(if (isCustomer) "Shop" else "POS")
                        Text(
                            "${role ?: "-"}  •  Pending sync: $pending",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                actions = {
                    TextButton(onClick = vm::refresh) { Text("Refresh") }
                    var menu by remember { mutableStateOf(false) }
                    Box {
                        TextButton(onClick = { menu = true }) { Text("Menu") }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            if (isCustomer) {
                                DropdownMenuItem(text = { Text("My orders") },
                                    onClick = { menu = false; onOrders() })
                            } else {
                                DropdownMenuItem(text = { Text("Stock") },
                                    onClick = { menu = false; onStock() })
                            }
                            if (isManager) {
                                DropdownMenuItem(text = { Text("Add product") },
                                    onClick = { menu = false; onAddProduct() })
                                DropdownMenuItem(text = { Text("Manage / Reports") },
                                    onClick = { menu = false; onManage() })
                            }
                            DropdownMenuItem(text = { Text("Logout") },
                                onClick = { menu = false; onLogout() })
                        }
                    }
                }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).padding(12.dp)) {
            OutlinedTextField(
                query, { vm.query.value = it },
                label = { Text("Search name / barcode") },
                modifier = Modifier.fillMaxWidth()
            )
            LazyColumn(Modifier.weight(1f)) {
                items(products, key = { it.id }) { p ->
                    ListItem(
                        headlineContent = { Text(p.name) },
                        supportingContent = { Text("৳%.2f  •  stock %d".format(p.price, p.stock)) },
                        trailingContent = {
                            Button(onClick = { vm.add(p) }, enabled = p.stock > 0) {
                                Text(if (p.stock > 0) "Add" else "Out")
                            }
                        }
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

            // Customers cannot give themselves a discount
            if (!isCustomer) {
                OutlinedTextField(
                    discount, { vm.discount.value = it },
                    label = { Text("Discount") }, modifier = Modifier.fillMaxWidth()
                )
            }
            Text(
                "Subtotal %.2f   Total %.2f".format(subtotal, total),
                style = MaterialTheme.typography.titleMedium
            )
            Button(
                onClick = vm::checkout,
                enabled = cart.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (isCustomer) "Buy now" else "Checkout") }
        }
    }
}
