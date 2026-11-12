package smart_inventory_backend.auth

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val users: UserRepository,
    private val encoder: PasswordEncoder,
    private val jwt: JwtService
) {
    @Transactional(readOnly = true)
    fun login(req: LoginRequest): AuthResponse {
        val user = users.findByUsername(normalize(req.username))
            ?.takeIf { encoder.matches(req.password, it.passwordHash) }
            ?: throw InvalidCredentialsException()
        return tokenFor(user)
    }

    /**
     * Public sign-up always creates a CUSTOMER. Staff accounts are never created here.
     *
     * The lookup gives a clear answer in the usual case, but two sign-ups for the same name can
     * both pass it at the same moment. The unique key on username then rejects the second insert,
     * and that is reported as the same USERNAME_TAKEN instead of a server error.
     */
    @Transactional
    fun register(req: RegisterRequest): AuthResponse {
        val username = normalize(req.username)
        if (users.findByUsername(username) != null) throw UsernameTakenException()
        val hash = encoder.encode(req.password)
        val user = try {
            users.saveAndFlush(AppUser(username, hash, Role.CUSTOMER))
        } catch (e: DataIntegrityViolationException) {
            throw UsernameTakenException()
        }
        return tokenFor(user)
    }

    private fun tokenFor(user: AppUser) = AuthResponse(jwt.create(user.username, user.role), user.role)

    private fun normalize(username: String) = username.trim().lowercase()
}
