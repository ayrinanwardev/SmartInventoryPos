package smart_inventory_backend.concurrency

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import smart_inventory_backend.auth.AuthService
import smart_inventory_backend.auth.RegisterRequest
import smart_inventory_backend.auth.UsernameTakenException

/** Two people (or one double tap) signing up with the same username at the same moment. */
class RegisterConcurrencyTest : ConcurrencyTestBase() {

    @Autowired private lateinit var auth: AuthService

    @Test
    fun `the same username registered many times at once gives one account and 409 for the rest`() {
        val results = runConcurrently(10) { auth.register(RegisterRequest("rahim", "rahim-secret")) }

        assertEquals(1, results.count { it.isSuccess })
        assertTrue(results.filter { it.isFailure }.all { it.exceptionOrNull() is UsernameTakenException }) {
            "Losers must get USERNAME_TAKEN, not a database error: " +
                results.mapNotNull { it.exceptionOrNull()?.let { e -> e::class.simpleName } }.distinct()
        }
        assertEquals(1, jdbc.queryForObject("select count(*) from users where username = 'rahim'", Int::class.java))
    }
}
