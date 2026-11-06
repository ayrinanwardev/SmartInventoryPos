package com.example.smartinventorypos.data.repo

import com.example.smartinventorypos.domain.common.AppResult
import com.example.smartinventorypos.domain.common.DomainError
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException

/** The one place that turns network and storage exceptions into [DomainError]s. */
internal suspend fun <T> safeCall(block: suspend () -> T): AppResult<T> = try {
    AppResult.Success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    AppResult.Failure(e.toDomainError())
}

private fun Exception.toDomainError(): DomainError = when (this) {
    is HttpException -> when (val status = code()) {
        400 -> DomainError.BadRequest
        401 -> DomainError.Unauthorized
        403 -> DomainError.Forbidden
        404 -> DomainError.NotFound
        409 -> DomainError.Conflict
        else -> DomainError.Server(status)
    }
    is IOException -> DomainError.Network
    else -> DomainError.Unknown
}
