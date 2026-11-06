package smart_inventory_backend.auth

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class LoginRequest(
    @field:NotBlank val username: String,
    @field:NotBlank val password: String
)

data class RegisterRequest(
    @field:NotBlank @field:Size(min = 3, max = 30) val username: String,
    @field:NotBlank @field:Size(min = 6, max = 100) val password: String
)

data class AuthResponse(val token: String, val role: Role)
