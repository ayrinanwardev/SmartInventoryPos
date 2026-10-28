package smart_inventory_backend.web

import jakarta.validation.constraints.NotBlank

data class LoginReq(
    @field:NotBlank val username: String,
    @field:NotBlank val password: String
)