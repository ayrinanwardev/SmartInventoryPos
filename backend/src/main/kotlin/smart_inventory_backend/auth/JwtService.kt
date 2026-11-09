package smart_inventory_backend.auth

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Date
import javax.crypto.SecretKey

private const val TOKEN_LIFETIME_MILLIS = 24L * 3600 * 1000

private const val DEV_SECRET = "dev-only-secret-change-me-32-bytes-min!!"

@Service
class JwtService(
    // JWT_SECRET from the environment or backend/.env (at least 32 characters) in real use.
    @Value("\${JWT_SECRET:}") secret: String
) {
    private val key: SecretKey = Keys.hmacShaKeyFor(
        secret.ifBlank {
            LoggerFactory.getLogger(JwtService::class.java)
                .warn("JWT_SECRET is not set: using the built-in dev secret. Do not use this in production.")
            DEV_SECRET
        }.toByteArray()
    )

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
