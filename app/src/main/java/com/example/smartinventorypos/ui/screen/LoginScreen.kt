package com.example.smartinventorypos.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartinventorypos.R
import com.example.smartinventorypos.ui.common.resolve
import com.example.smartinventorypos.ui.components.ErrorBanner
import com.example.smartinventorypos.ui.components.PosTextField
import com.example.smartinventorypos.ui.components.PrimaryButton
import com.example.smartinventorypos.ui.components.QuietButton
import com.example.smartinventorypos.ui.theme.Brown
import com.example.smartinventorypos.ui.theme.Muted
import com.example.smartinventorypos.ui.theme.Yellow
import com.example.smartinventorypos.ui.viewmodel.LoginViewModel

@Composable
fun LoginScreen(modifier: Modifier = Modifier, vm: LoginViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val register = state.isRegistering

    Box(
        modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.widthIn(max = 420.dp).fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(Modifier.size(56.dp).clip(CircleShape).background(Yellow), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Storefront, contentDescription = null, tint = Brown)
            }
            Column(Modifier.padding(top = 12.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(if (register) R.string.register_title else R.string.login_title),
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    stringResource(if (register) R.string.register_subtitle else R.string.login_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Muted
                )
            }
            PosTextField(
                state.username,
                vm::onUsernameChange,
                stringResource(R.string.username),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            PosTextField(
                state.password,
                vm::onPasswordChange,
                stringResource(R.string.password),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done)
            )
            if (register) {
                Text(
                    stringResource(R.string.register_password_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted
                )
            }
            state.error?.let { ErrorBanner(it.resolve()) }

            PrimaryButton(
                text = stringResource(if (register) R.string.register_button else R.string.login_button),
                busy = state.busy,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                onClick = vm::submit
            )
            QuietButton(
                stringResource(if (register) R.string.register_switch else R.string.login_switch),
                onClick = vm::toggleMode,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}
