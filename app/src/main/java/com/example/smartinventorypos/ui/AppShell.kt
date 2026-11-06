package com.example.smartinventorypos.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartinventorypos.R
import com.example.smartinventorypos.ui.common.LocalSessionKey
import com.example.smartinventorypos.ui.common.resolve
import com.example.smartinventorypos.ui.components.BottomItem
import com.example.smartinventorypos.ui.components.PillBottomBar
import com.example.smartinventorypos.ui.navigation.Destination
import com.example.smartinventorypos.ui.navigation.bottomDestinations
import com.example.smartinventorypos.ui.screen.AddProductScreen
import com.example.smartinventorypos.ui.screen.LoginScreen
import com.example.smartinventorypos.ui.screen.ManageScreen
import com.example.smartinventorypos.ui.screen.MyOrdersScreen
import com.example.smartinventorypos.ui.screen.PosScreen
import com.example.smartinventorypos.ui.screen.StockScreen
import com.example.smartinventorypos.ui.theme.Brown
import com.example.smartinventorypos.ui.viewmodel.AppViewModel
import kotlinx.coroutines.launch

/** Decides what the signed-in user can reach, and hosts the bottom bar and messages. */
@Composable
fun AppShell(vm: AppViewModel = hiltViewModel()) {
    val session by vm.session.collectAsStateWithLifecycle()
    val rejected by vm.rejectedSales.collectAsStateWithLifecycle()
    val loggedIn = session != null
    val role = session?.role

    var requested by rememberSaveable { mutableStateOf(Destination.SELL) }
    val current = requested.takeIf { it.isAllowedFor(role) } ?: Destination.SELL

    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current

    LaunchedEffect(loggedIn) { if (!loggedIn) requested = Destination.SELL }
    LaunchedEffect(Unit) { vm.messages.collect { snack.showSnackbar(it.resolve(resources)) } }
    LaunchedEffect(rejected, loggedIn) {
        if (loggedIn && rejected > 0) {
            snack.showSnackbar(resources.getQuantityString(R.plurals.sales_rejected, rejected, rejected))
            vm.acknowledgeRejectedSales()
        }
    }

    BackHandler(enabled = loggedIn && current != Destination.SELL) { requested = current.parent }

    val bottomItems = bottomDestinations(role).map {
        BottomItem(it.destination, stringResource(it.label), it.icon)
    }

    Scaffold(
        containerColor = Color.White,
        snackbarHost = {
            SnackbarHost(snack) {
                Snackbar(it, containerColor = Brown, contentColor = Color.White, shape = RoundedCornerShape(16.dp))
            }
        },
        bottomBar = {
            if (loggedIn && current != Destination.ADD_PRODUCT && bottomItems.size > 1) {
                PillBottomBar(bottomItems, selected = current, onSelect = { requested = it })
            }
        }
    ) { padding ->
        val content = Modifier.padding(padding)
        if (!loggedIn) {
            LoginScreen(content)
        } else {
            CompositionLocalProvider(LocalSessionKey provides session?.token) {
                when (current) {
                    Destination.ADD_PRODUCT -> AddProductScreen(content, onDone = { requested = Destination.MANAGE })
                    Destination.MANAGE -> ManageScreen(content, onAddProduct = { requested = Destination.ADD_PRODUCT })
                    Destination.STOCK -> StockScreen(content)
                    Destination.ORDERS -> MyOrdersScreen(content)
                    Destination.SELL -> PosScreen(
                        modifier = content,
                        onLogout = vm::logout,
                        onMessage = { message ->
                            scope.launch { snack.showSnackbar(message.resolve(resources)) }
                        }
                    )
                }
            }
        }
    }
}
