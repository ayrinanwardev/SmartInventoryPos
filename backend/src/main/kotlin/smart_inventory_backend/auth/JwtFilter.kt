package smart_inventory_backend.auth

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

private const val BEARER_PREFIX = "Bearer "

@Component
class JwtFilter(private val jwt: JwtService) : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(JwtFilter::class.java)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val header = request.getHeader("Authorization")
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            try {
                val claims = jwt.parse(header.removePrefix(BEARER_PREFIX))
                val role = claims.get("role", String::class.java)
                SecurityContextHolder.getContext().authentication =
                    UsernamePasswordAuthenticationToken(
                        claims.subject, null, listOf(SimpleGrantedAuthority("ROLE_$role"))
                    )
            } catch (e: Exception) {
                log.warn("JWT validation failed: ${e.message}")
                SecurityContextHolder.clearContext()
            }
        }
        filterChain.doFilter(request, response)
    }
}
