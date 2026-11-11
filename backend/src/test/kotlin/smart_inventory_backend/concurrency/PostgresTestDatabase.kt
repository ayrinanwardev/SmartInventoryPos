package smart_inventory_backend.concurrency

import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.PostgreSQLContainer

/**
 * A real PostgreSQL for tests that depend on its locking. H2 doesn't lock rows or detect deadlocks
 * the way PostgreSQL does, so those tests would prove nothing there.
 *
 * Uses TEST_DB_URL / TEST_DB_USERNAME / TEST_DB_PASSWORD when set (a throwaway local database),
 * otherwise starts a PostgreSQL container (GitHub Actions has Docker). With neither, tests are skipped.
 * It never falls back to the app's own database.
 */
object PostgresTestDatabase {
    private val envUrl: String? = System.getenv("TEST_DB_URL")?.takeIf { it.isNotBlank() }

    private val container: PostgreSQLContainer<*> by lazy {
        PostgreSQLContainer("postgres:17-alpine").also { it.start() }
    }

    /** Used by @EnabledIf, before any Spring context is created. */
    @JvmStatic
    fun available(): Boolean =
        envUrl != null || runCatching { DockerClientFactory.instance().isDockerAvailable }.getOrDefault(false)

    fun register(registry: DynamicPropertyRegistry) {
        if (envUrl != null) {
            registry.add("spring.datasource.url") { envUrl }
            registry.add("spring.datasource.username") { System.getenv("TEST_DB_USERNAME") ?: "postgres" }
            registry.add("spring.datasource.password") { System.getenv("TEST_DB_PASSWORD") ?: "" }
        } else {
            registry.add("spring.datasource.url") { container.jdbcUrl }
            registry.add("spring.datasource.username") { container.username }
            registry.add("spring.datasource.password") { container.password }
        }
        // Enough connections for every worker thread to hold one at the same time.
        registry.add("spring.datasource.hikari.maximum-pool-size") { "24" }
    }
}
