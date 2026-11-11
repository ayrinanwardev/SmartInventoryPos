package smart_inventory_backend

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.mock.env.MockEnvironment
import smart_inventory_backend.auth.JwtService
import smart_inventory_backend.auth.Role

/** The built-in dev secret is public, so it must never sign tokens outside the dev profile. */
class JwtSecretTest {

    private val realSecret = "a-real-secret-that-is-at-least-32-characters-long"

    @Test
    fun `start-up fails without JWT_SECRET outside the dev profile`() {
        val error = assertThrows<IllegalStateException> { JwtService("", MockEnvironment()) }
        assertTrue(error.message!!.contains("JWT_SECRET is not set"))
    }

    @Test
    fun `the dev profile may use the built-in secret`() {
        val dev = MockEnvironment().apply { setActiveProfiles("dev") }
        val jwt = JwtService("", dev)
        assertEquals("cashier", jwt.parse(jwt.create("cashier", Role.CASHIER)).subject)
    }

    @Test
    fun `a configured secret works in any profile`() {
        val jwt = JwtService(realSecret, MockEnvironment())
        val claims = jwt.parse(jwt.create("manager", Role.MANAGER))
        assertEquals("manager", claims.subject)
        assertEquals("MANAGER", claims["role"])
    }

    @Test
    fun `a token signed with the dev secret is rejected by a server with a real secret`() {
        val forged = JwtService("", MockEnvironment().apply { setActiveProfiles("dev") }).create("manager", Role.MANAGER)
        assertThrows<Exception> { JwtService(realSecret, MockEnvironment()).parse(forged) }
    }
}
