package com.example.smartinventorypos.domain.common

/** The outcome of an operation that can fail in an expected way. */
sealed interface AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>
    data class Failure(val error: DomainError) : AppResult<Nothing>
}

/** Expected failures, free of HTTP and Android types, so the layers above can decide what they mean. */
sealed interface DomainError {
    data object Network : DomainError
    data object Unauthorized : DomainError
    data object Forbidden : DomainError
    data object BadRequest : DomainError
    data object NotFound : DomainError
    data object Conflict : DomainError
    data class Server(val code: Int) : DomainError
    data object Unknown : DomainError
    data object EmptyCart : DomainError
    data object InvalidDiscount : DomainError

    /** True if sending the same sale again can never succeed, so it must go to review instead of retrying. */
    val isPermanentRejection: Boolean
        get() = this is BadRequest || this is NotFound || this is Conflict
}
