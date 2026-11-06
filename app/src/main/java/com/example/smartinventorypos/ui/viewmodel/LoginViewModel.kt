package com.example.smartinventorypos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartinventorypos.R
import com.example.smartinventorypos.domain.common.AppResult
import com.example.smartinventorypos.domain.common.DomainError
import com.example.smartinventorypos.domain.repository.AuthRepository
import com.example.smartinventorypos.ui.common.UiText
import com.example.smartinventorypos.ui.common.toUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val isRegistering: Boolean = false,
    val username: String = "",
    val password: String = "",
    val busy: Boolean = false,
    val error: UiText? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(private val auth: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onUsernameChange(value: String) = _state.update { it.copy(username = value) }
    fun onPasswordChange(value: String) = _state.update { it.copy(password = value) }
    fun toggleMode() = _state.update { it.copy(isRegistering = !it.isRegistering, error = null) }

    fun submit() {
        val form = _state.value
        if (form.busy) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            val username = form.username.trim()
            val result =
                if (form.isRegistering) auth.register(username, form.password)
                else auth.login(username, form.password)
            when (result) {
                // Don't keep the password in memory once it has been used.
                is AppResult.Success -> _state.value = LoginUiState()
                is AppResult.Failure -> _state.update {
                    it.copy(busy = false, error = messageFor(result.error, form.isRegistering))
                }
            }
        }
    }

    private fun messageFor(error: DomainError, registering: Boolean): UiText = when {
        !registering && error is DomainError.Unauthorized -> UiText(R.string.error_bad_login)
        registering && error is DomainError.Conflict -> UiText(R.string.error_username_taken)
        registering && error is DomainError.BadRequest -> UiText(R.string.error_bad_registration)
        else -> error.toUiText()
    }
}
