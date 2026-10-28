package com.example.smartinventorypos

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresExtension
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.smartinventorypos.data.remote.Session
import com.example.smartinventorypos.ui.PosViewModel
import com.example.smartinventorypos.ui.screen.AddProductScreen
import com.example.smartinventorypos.ui.screen.LoginScreen
import com.example.smartinventorypos.ui.screen.ManageScreen
import com.example.smartinventorypos.ui.screen.MyOrdersScreen
import com.example.smartinventorypos.ui.screen.PosScreen
import com.example.smartinventorypos.ui.screen.StockScreen

class MainActivity : ComponentActivity() {
    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val vm: PosViewModel = viewModel(factory = viewModelFactory {
                    val vm: PosViewModel =
                        viewModel(factory = viewModelFactory {
                            initializer { PosViewModel(ServiceLocator.repo) }
                        })

                    var loggedIn by remember { mutableStateOf(Session.token != null) }
                    var role by remember { mutableStateOf(Session.role) }
                    var screen by remember { mutableStateOf("pos") }
                    val isStaff = role == "MANAGER" || role == "CASHIER"

                    when {
                        !loggedIn -> LoginScreen(vm) {
                            role = Session.role
                            screen = "pos"
                            loggedIn = true
                            vm.refresh()
                        }

                        screen == "add" && role == "MANAGER" -> AddProductScreen(vm) {
                            screen = "pos"
                        }

                        screen == "manage" && role == "MANAGER" -> ManageScreen(vm) {
                            screen = "pos"
                        }

                        screen == "stock" && isStaff -> StockScreen(vm, role) { screen = "pos" }
                        screen == "orders" && role == "CUSTOMER" -> MyOrdersScreen(vm) {
                            screen = "pos"
                        }

                        else -> PosScreen(
                            vm = vm,
                            role = role,
                            onAddProduct = { screen = "add" },
                            onManage = { screen = "manage" },
                            onStock = { screen = "stock" },
                            onOrders = { screen = "orders" },
                            onLogout = {
                                Session.clear()
                                vm.clearCart()
                                role = null
                                screen = "pos"
                                loggedIn = false
                            }
                        )
                    }
                })
            }
        }


    }
}


