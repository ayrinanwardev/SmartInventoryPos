package smart_inventory_backend.auth

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Date
import javax.crypto.SecretKey

private const val TOKEN_LIFETIME_MILLIS = 24L * 3600 * 1000

@Service
class JwtService {
    // Set the JWT_SECRET environment variable in real use (at least 32 characters).
    private val key: SecretKey = Keys.hmacShaKeyFor(
        (System.getenv("JWT_SECRET") ?: run {
            LoggerFactory.getLogger(JwtService::class.java)
                .warn("JWT_SECRET is not set: using the built-in dev secret. Do not use this in production.")
            "dev-only-secret-change-me-32-bytes-min!!"
        }).toByteArray()
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
