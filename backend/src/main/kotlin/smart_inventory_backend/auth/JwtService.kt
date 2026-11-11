package smart_inventory_backend.auth

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles
import org.springframework.stereotype.Service
import java.util.Date
import javax.crypto.SecretKey

private const val TOKEN_LIFETIME_MILLIS = 24L * 3600 * 1000

private const val DEV_SECRET = "dev-only-secret-change-me-32-bytes-min!!"

@Service
class JwtService(
    // JWT_SECRET from the environment or backend/.env (at least 32 characters) in real use.
    @Value("\${JWT_SECRET:}") secret: String,
    environment: Environment
) {
    private val key: SecretKey = Keys.hmacShaKeyFor(resolveSecret(secret, environment).toByteArray())

    /**
     * The built-in secret is public (it's in this file), so tokens signed with it can be forged.
     * It is allowed only under the dev profile; anywhere else a missing secret stops start-up.
     */
    private fun resolveSecret(secret: String, environment: Environment): String {
        if (secret.isNotBlank()) return secret
        check(environment.acceptsProfiles(Profiles.of("dev"))) {
            "JWT_SECRET is not set. Set it to a random value of at least 32 characters " +
                "(for example: openssl rand -base64 48), or run with the dev profile for local development."
        }
        LoggerFactory.getLogger(JwtService::class.java)
            .warn("JWT_SECRET is not set: using the built-in dev secret (dev profile only).")
        return DEV_SECRET
    }

    fun create(username: String, role: Role): String =
        Jwts.builder()
            .subject(username)
            .claim("role", role.name)
            .expiration(Date(System.currentTimeMillis() + TOKEN_LIFETIME_MILLIS))
            .signWith(key)
            .compact()

    fun parse(token: String): Claims =
        Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload
}
