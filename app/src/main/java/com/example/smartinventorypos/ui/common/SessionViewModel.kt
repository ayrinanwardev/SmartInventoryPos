package com.example.smartinventorypos.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel

/** Identifies the current login. AppShell provides it. */
val LocalSessionKey = staticCompositionLocalOf<String?> { null }

/**
 * A ViewModel that belongs to the current login, so one user's cart and lists can't reach the next user.
 * A new login gets new instances. Old ones stay idle in memory until the activity finishes.
 * The class name is part of the key because the ViewModel store treats a key as the full identity.
 */
@Composable
inline fun <reified VM : ViewModel> sessionViewModel(): VM =
    hiltViewModel(key = "${VM::class.java.name}:${LocalSessionKey.current}")
