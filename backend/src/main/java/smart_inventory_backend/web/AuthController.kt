package smart_inventory_backend.web

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import smart_inventory_backend.model.AppUser
import smart_inventory_backend.repo.UserRepository
import smart_inventory_backend.security.JwtService

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val users: UserRepository,
    private val encoder: PasswordEncoder,
    private val jwt: JwtService
) {
    @PostMapping("/login")
    fun login(@Valid @RequestBody req: LoginReq): LoginRes {
        val name = req.username.trim().lowercase()
        val user = users.findByUsername(name)
            ?.takeIf { encoder.matches(req.password, it.passwordHash) }
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bad credentials")
        return LoginRes(jwt.create(user.username, user.role), user.role)
    }

    /** Public sign-up. Always creates a CUSTOMER (managers/cashiers are created by seed/admin only). */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@Valid @RequestBody req: RegisterReq): LoginRes {
        val name = req.username.trim().lowercase()
        if (users.findByUsername(name) != null) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Username already taken")
        }
        val user = users.save(
            AppUser(username = name, passwordHash = encoder.encode(req.password), role = "CUSTOMER")
        )
        return LoginRes(jwt.create(user.username, user.role), user.role)
    }
}

