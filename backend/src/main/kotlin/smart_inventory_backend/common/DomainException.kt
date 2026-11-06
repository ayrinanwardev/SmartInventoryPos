package smart_inventory_backend.common

/**
 * A broken business rule. The domain only says which kind of failure happened;
 * [GlobalExceptionHandler] alone decides what that means in HTTP.
 */
sealed class DomainException(val code: String, message: String) : RuntimeException(message)

open class NotFoundException(code: String, message: String) : DomainException(code, message)
open class ConflictException(code: String, message: String) : DomainException(code, message)
open class InvalidRequestException(code: String, message: String) : DomainException(code, message)
open class ForbiddenException(code: String, message: String) : DomainException(code, message)
open class UnauthorizedException(code: String, message: String) : DomainException(code, message)
