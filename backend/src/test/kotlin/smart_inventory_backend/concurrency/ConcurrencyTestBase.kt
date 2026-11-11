package smart_inventory_backend.concurrency

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.condition.EnabledIf
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import smart_inventory_backend.auth.Actor
import smart_inventory_backend.auth.AppUser
import smart_inventory_backend.auth.Role
import smart_inventory_backend.auth.UserRepository
import smart_inventory_backend.product.Product
import smart_inventory_backend.product.ProductRepository
import smart_inventory_backend.sale.SaleItemRequest
import smart_inventory_backend.sale.SaleRequest
import java.math.BigDecimal
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Base for tests that run many requests at the same moment against real PostgreSQL.
 * Each test starts from empty tables with one manager and one cashier.
 */
@SpringBootTest
@EnabledIf("smart_inventory_backend.concurrency.PostgresTestDatabase#available")
abstract class ConcurrencyTestBase {

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun database(registry: DynamicPropertyRegistry) {
            PostgresTestDatabase.register(registry)
            registry.add("JWT_SECRET") { "concurrency-test-secret-at-least-32-characters" }
        }
    }

    @Autowired protected lateinit var jdbc: JdbcTemplate
    @Autowired private lateinit var users: UserRepository
    @Autowired private lateinit var productRepository: ProductRepository
    @Autowired private lateinit var encoder: PasswordEncoder

    protected val manager = Actor("manager", Role.MANAGER)
    protected val cashier = Actor("cashier", Role.CASHIER)

    @BeforeEach
    fun resetDatabase() {
        jdbc.execute("TRUNCATE sale_items, sales, stock_adjustments, products, users RESTART IDENTITY CASCADE")
        users.save(AppUser("manager", encoder.encode("not-used"), Role.MANAGER))
        users.save(AppUser("cashier", encoder.encode("not-used"), Role.CASHIER))
    }

    protected fun product(name: String, stock: Int, barcode: String? = null, price: String = "80.00"): Long =
        requireNotNull(productRepository.saveAndFlush(Product(name, barcode, BigDecimal(price), stock)).id)

    protected fun stockOf(productId: Long): Int =
        jdbc.queryForObject("select stock from products where id = ?", Int::class.java, productId)!!

    protected fun rowCount(table: String): Int =
        jdbc.queryForObject("select count(*) from $table", Int::class.java)!!

    protected fun sale(vararg items: Pair<Long, Int>, clientId: UUID = UUID.randomUUID()) =
        SaleRequest(clientId, items.map { (id, qty) -> SaleItemRequest(id, qty) })

    /**
     * Runs [task] on [threads] threads that all start at the same instant, and returns each
     * thread's outcome. Waiting on a latch first makes the calls overlap as much as possible.
     */
    protected fun <T> runConcurrently(threads: Int, task: (index: Int) -> T): List<Result<T>> {
        val pool = Executors.newFixedThreadPool(threads)
        val ready = CountDownLatch(threads)
        val start = CountDownLatch(1)
        try {
            val futures = (0 until threads).map { i ->
                pool.submit(Callable {
                    ready.countDown()
                    start.await()
                    runCatching { task(i) }
                })
            }
            check(ready.await(30, TimeUnit.SECONDS)) { "Worker threads did not start" }
            start.countDown()
            return futures.map { it.get(120, TimeUnit.SECONDS) }
        } finally {
            pool.shutdownNow()
        }
    }

    /** Fails with every unexpected error listed, which is more useful than the first one alone. */
    protected fun assertNoFailures(results: List<Result<*>>) {
        val failures = results.mapNotNull { it.exceptionOrNull() }
        check(failures.isEmpty()) {
            "${failures.size} of ${results.size} calls failed:\n" +
                failures.groupingBy { "${it::class.simpleName}: ${it.message?.take(160)}" }.eachCount()
                    .entries.joinToString("\n") { (msg, n) -> "  $n × $msg" }
        }
    }
}
