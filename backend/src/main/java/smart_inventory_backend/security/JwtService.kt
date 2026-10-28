package smart_inventory_backend.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Service
import java.util.Date
import javax.crypto.SecretKey

@Service
class JwtService {
    // Set the JWT_SECRET environment variable in real use (at least 32 characters).
    private val key: SecretKey = Keys.hmacShaKeyFor(
        (System.getenv("JWT_SECRET") ?: "dev-only-secret-change-me-32-bytes-min!!").toByteArray()
    )

    fun create(username: String, role: String): String =
        Jwts.builder()
            .subject(username)
            .claim("role", role)
            .expiration(Date(System.currentTimeMillis() + 24L * 3600 * 1000))
            .signWith(key)
            .compact()

    fun parse(token: String): Claims =
        Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload
}