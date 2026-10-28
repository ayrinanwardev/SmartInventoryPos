package com.example.smartinventorypos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.smartinventorypos.data.remote.Session
import com.example.smartinventorypos.ui.PosViewModel
import com.example.smartinventorypos.ui.screen.LoginScreen
import com.example.smartinventorypos.ui.screen.PosScreen
import com.example.smartinventorypos.ui.theme.SmartInventoryPOSTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val vm: PosViewModel =
                    viewModel(factory = viewModelFactory {
                    initializer { PosViewModel(ServiceLocator.repo) }
                })
                var loggedIn by remember { mutableStateOf(Session.token != null) }
                if (loggedIn) PosScreen(vm) else LoginScreen(vm) { loggedIn = true; vm.refresh() }
            }
        }
    }
}
