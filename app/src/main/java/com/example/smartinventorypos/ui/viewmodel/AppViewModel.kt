package com.example.smartinventorypos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartinventorypos.domain.common.AppResult
import com.example.smartinventorypos.domain.model.UserSession
import com.example.smartinventorypos.domain.repository.AuthRepository
import com.example.smartinventorypos.domain.repository.OfflineSaleRepository
import com.example.smartinventorypos.domain.repository.SessionStore
import com.example.smartinventorypos.domain.usecase.RefreshCatalogUseCase
import com.example.smartinventorypos.ui.common.UiText
import com.example.smartinventorypos.ui.common.toUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** State the whole app shares: who is signed in, and messages not tied to one screen. */
@HiltViewModel
class AppViewModel @Inject constructor(
    sessions: SessionStore,
    private val auth: AuthRepository,
    private val sales: OfflineSaleRepository,
    private val refreshCatalog: RefreshCatalogUseCase
) : ViewModel() {

    val session: StateFlow<UserSession?> = sessions.session

    val rejectedSales: StateFlow<Int> = sales.rejectedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _messages = MutableSharedFlow<UiText>(extraBufferCapacity = 4)
    val messages: SharedFlow<UiText> = _messages

    init {
        viewModelScope.launch {
            sessions.session.map { it?.token }.distinctUntilChanged().filterNotNull().collect {
                val result = refreshCatalog()
                if (result is AppResult.Failure) _messages.emit(result.error.toUiText())
            }
        }
    }

    fun acknowledgeRejectedSales() {
        viewModelScope.launch { sales.acknowledgeRejected() }
    }

    fun logout() = auth.logout()
}
