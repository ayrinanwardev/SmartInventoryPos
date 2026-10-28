package smart_inventory_backend.web

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RegisterReq(
    @field:NotBlank @field:Size(min = 3, max = 30) val username: String,
    @field:NotBlank @field:Size(min = 6, max = 100) val password: String
)
