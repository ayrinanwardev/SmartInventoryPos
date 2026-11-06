package smart_inventory_backend.common

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/** The one error format every handled failure uses. */
data class ApiError(
    val code: String,
    val message: String,
    val details: List<String> = emptyList()
)

/**
 * Deliberately has no catch-all handler: security exceptions must keep reaching Spring Security
 * so they stay 401/403, and unexpected failures should stay visible as 500s.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(DomainException::class)
    fun handleDomain(e: DomainException): ResponseEntity<ApiError> =
        ResponseEntity.status(statusOf(e)).body(ApiError(e.code, e.message.orEmpty()))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException): ResponseEntity<ApiError> {
        val details = e.bindingResult.fieldErrors.map { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity.badRequest()
            .body(ApiError("VALIDATION_FAILED", "The request has invalid fields", details))
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadable(e: HttpMessageNotReadableException): ResponseEntity<ApiError> =
        ResponseEntity.badRequest().body(ApiError("MALFORMED_REQUEST", "The request body could not be read"))

    private fun statusOf(e: DomainException): HttpStatus = when (e) {
        is NotFoundException -> HttpStatus.NOT_FOUND
        is ConflictException -> HttpStatus.CONFLICT
        is InvalidRequestException -> HttpStatus.BAD_REQUEST
        is ForbiddenException -> HttpStatus.FORBIDDEN
        is UnauthorizedException -> HttpStatus.UNAUTHORIZED
    }
}
