package smart_inventory_backend.auth

import smart_inventory_backend.common.ConflictException
import smart_inventory_backend.common.UnauthorizedException

class InvalidCredentialsException : UnauthorizedException("INVALID_CREDENTIALS", "Bad credentials")

class UnknownUserException(username: String) : UnauthorizedException("UNKNOWN_USER", "Unknown user: $username")

class UsernameTakenException : ConflictException("USERNAME_TAKEN", "Username already taken")
