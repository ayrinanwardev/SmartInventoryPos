package com.example.smartinventorypos.ui.common

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.smartinventorypos.R
import com.example.smartinventorypos.domain.common.DomainError

/** Text chosen by a ViewModel, which is turned into a string only when the UI displays it. */
class UiText(@param:StringRes val id: Int, vararg val args: Any)

@Composable
fun UiText.resolve(): String = stringResource(id, *args)

fun UiText.resolve(resources: Resources): String = resources.getString(id, *args)

/** The standard message for an error. A ViewModel can check for a specific error first to say more. */
fun DomainError.toUiText(): UiText = when (this) {
    DomainError.Unauthorized -> UiText(R.string.error_session_expired)
    DomainError.Forbidden -> UiText(R.string.error_forbidden)
    DomainError.Conflict -> UiText(R.string.error_conflict)
    DomainError.BadRequest -> UiText(R.string.error_with_code, 400)
    DomainError.NotFound -> UiText(R.string.error_with_code, 404)
    is DomainError.Server -> UiText(R.string.error_with_code, code)
    DomainError.Network -> UiText(R.string.error_network)
    DomainError.Unknown -> UiText(R.string.error_unknown)
    DomainError.EmptyCart -> UiText(R.string.error_empty_cart)
    DomainError.InvalidDiscount -> UiText(R.string.error_invalid_discount)
}
