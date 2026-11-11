package com.example.smartinventorypos.concurrency

import com.example.smartinventorypos.domain.common.AppResult
import com.example.smartinventorypos.domain.common.DomainError
import com.example.smartinventorypos.domain.model.Cart
import com.example.smartinventorypos.domain.model.Money
import com.example.smartinventorypos.domain.model.PendingSale
import com.example.smartinventorypos.domain.model.PendingSaleLine
import com.example.smartinventorypos.domain.model.Product
import com.example.smartinventorypos.domain.model.Role
import com.example.smartinventorypos.domain.model.SyncStatus
import com.example.smartinventorypos.domain.model.UserSession
import com.example.smartinventorypos.domain.repository.OfflineSaleRepository
import com.example.smartinventorypos.domain.repository.ProductCatalogRepository
import com.example.smartinventorypos.domain.repository.SaleSyncScheduler
import com.example.smartinventorypos.domain.repository.SessionStore
import com.example.smartinventorypos.domain.usecase.CheckoutUseCase
import com.example.smartinventorypos.domain.usecase.RefreshCatalogUseCase
import com.example.smartinventorypos.domain.usecase.SyncPendingSalesUseCase
import com.example.smartinventorypos.ui.viewmodel.PosViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

private val tea = Product(1, "Black Tea 400g", null, Money.ofMinor(21_000), stock = 10_000)

private class Sessions(role: Role) : SessionStore {
    override val session: StateFlow<UserSession?> = MutableStateFlow(UserSession("token", role))
    override fun save(session: UserSession) = Unit
    override fun clear() = Unit
    override fun clearIfCurrent(token: String) = Unit
}

private class NoCatalog : ProductCatalogRepository {
    override fun search(query: String): Flow<List<Product>> = emptyFlow()
    override suspend fun refreshFromServer(): AppResult<Unit> = AppResult.Success(Unit)
}

private class CountingScheduler : SaleSyncScheduler {
    val scheduled = AtomicInteger()
    override fun schedule() {
        scheduled.incrementAndGet()
    }
}

/**
 * A thread-safe stand-in for the phone's Room outbox plus the server. The server keeps one sale per
 * clientId, like the backend's unique key, and counts every upload it receives.
 */
private class OutboxAndServer(private val saveGate: CompletableDeferred<Unit>? = null) : OfflineSaleRepository {
    val local = ConcurrentHashMap<String, SyncStatus>()
    val server = ConcurrentHashMap<String, PendingSale>()
    val uploads = AtomicInteger()
    val recorded = AtomicInteger()

    override fun pendingCount(): Flow<Int> = emptyFlow()
    override fun rejectedCount(): Flow<Int> = flowOf(0)
    override suspend fun hasPending() = local.containsValue(SyncStatus.PENDING)
    override suspend fun acknowledgeRejected() = Unit

    override suspend fun record(cart: Cart, discount: Money): AppResult<Unit> {
        saveGate?.await() // Holds the first save open, like a slow disk write.
        recorded.incrementAndGet()
        local[UUID.randomUUID().toString()] = SyncStatus.PENDING
        return AppResult.Success(Unit)
    }

    override suspend fun pendingSales(): List<PendingSale> =
        local.filterValues { it == SyncStatus.PENDING }.keys.map {
            PendingSale(it, listOf(PendingSaleLine(tea.id, 1)), Money.ZERO)
        }

    override suspend fun upload(sale: PendingSale): AppResult<Unit> {
        uploads.incrementAndGet()
        yield() // Lets other workers interleave between reading the outbox and uploading.
        server.putIfAbsent(sale.clientId, sale)
        return AppResult.Success(Unit)
    }

    override suspend fun markStatus(clientId: String, status: SyncStatus) {
        local[clientId] = status
    }
}

/** A double tap, or a tap while the first save is still writing, must not record two sales. */
@OptIn(ExperimentalCoroutinesApi::class)
class PosViewModelDoubleTapTest {
    private val main = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(main)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `tapping checkout five times while the first save is running saves one sale`() = runTest(main) {
        val gate = CompletableDeferred<Unit>()
        val outbox = OutboxAndServer(saveGate = gate)
        val sessions = Sessions(Role.CASHIER)
        val scheduler = CountingScheduler()
        val vm = PosViewModel(
            sessions, NoCatalog(), outbox,
            CheckoutUseCase(sessions, outbox, scheduler),
            RefreshCatalogUseCase(sessions, outbox, NoCatalog(), scheduler)
        )
        vm.add(tea)

        repeat(5) { vm.checkout() }
        advanceUntilIdle() // The first save is now waiting on the gate; the other taps were ignored.
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, outbox.recorded.get())
        assertEquals(1, scheduler.scheduled.get())
    }
}

/** The cart is immutable and changed with StateFlow.update, which retries on contention. */
class CartConcurrencyTest {
    @Test
    fun `adds from many threads at once are never lost`() {
        val cart = MutableStateFlow(Cart())
        val threads = 8
        val addsPerThread = 250
        val pool = Executors.newFixedThreadPool(threads)
        val start = CountDownLatch(1)
        repeat(threads) {
            pool.execute {
                start.await()
                repeat(addsPerThread) { cart.update { it.add(tea) } }
            }
        }
        start.countDown()
        pool.shutdown()
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS))

        assertEquals(threads * addsPerThread, cart.value.quantityOf(tea.id))
        assertEquals(tea.price * (threads * addsPerThread), cart.value.subtotal)
    }

    @Test
    fun `concurrent adds stop exactly at the product's stock`() {
        val scarce = tea.copy(stock = 100)
        val cart = MutableStateFlow(Cart())
        val pool = Executors.newFixedThreadPool(8)
        val start = CountDownLatch(1)
        repeat(8) {
            pool.execute {
                start.await()
                repeat(50) { cart.update { it.add(scarce) } }
            }
        }
        start.countDown()
        pool.shutdown()
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS))

        assertEquals(100, cart.value.quantityOf(scarce.id))
    }
}

/**
 * Checkouts keep happening on real threads while sync runs overlap (in production WorkManager's
 * unique work makes overlap rare, but a manual refresh can still trigger one). No sale may be lost,
 * left pending, or counted twice on the server.
 */
class CheckoutWhileSyncingTest {
    @Test
    fun `every sale reaches the server exactly once while syncs overlap`() = runBlocking {
        val outbox = OutboxAndServer()
        val sessions = Sessions(Role.CASHIER)
        val scheduler = CountingScheduler()
        val checkout = CheckoutUseCase(sessions, outbox, scheduler)
        val sync = SyncPendingSalesUseCase(outbox, RefreshCatalogUseCase(sessions, outbox, NoCatalog(), scheduler))
        val cart = Cart().add(tea)
        val sales = 200

        val sellers = (1..sales).map { async(Dispatchers.Default) { checkout(cart, Money.ZERO) } }
        val syncers = (1..6).map {
            async(Dispatchers.Default) { repeat(20) { sync(); yield() } }
        }
        val results = sellers.awaitAll()
        syncers.awaitAll()
        sync() // The final run, as WorkManager would do after the last checkout scheduled it.

        assertTrue(results.all { it == AppResult.Success(Unit) })
        assertEquals(sales, outbox.local.size)
        assertTrue("Every sale must end SYNCED", outbox.local.values.all { it == SyncStatus.SYNCED })
        // Overlapping runs may upload the same sale twice; the clientId keeps it one sale on the server.
        assertEquals(sales, outbox.server.size)
        assertTrue(outbox.uploads.get() >= sales)
        assertTrue("Every checkout asks for a sync", scheduler.scheduled.get() >= sales)
    }

    @Test
    fun `a failing upload during overlapping syncs is retried, not lost`() = runBlocking {
        val outbox = OutboxAndServer()
        val failNext = AtomicInteger(50)
        val failing = object : OfflineSaleRepository by outbox {
            override suspend fun upload(sale: PendingSale): AppResult<Unit> =
                if (failNext.getAndDecrement() > 0) AppResult.Failure(DomainError.Network) else outbox.upload(sale)
        }
        val sessions = Sessions(Role.CASHIER)
        val scheduler = CountingScheduler()
        val checkout = CheckoutUseCase(sessions, failing, scheduler)
        val sync = SyncPendingSalesUseCase(failing, RefreshCatalogUseCase(sessions, failing, NoCatalog(), scheduler))
        val cart = Cart().add(tea)

        (1..100).map { async(Dispatchers.Default) { checkout(cart, Money.ZERO) } }.awaitAll()
        val firstRuns = (1..4).map { async(Dispatchers.Default) { sync() } }.awaitAll()
        while (!sync()) yield() // Keep retrying, as WorkManager's backoff would.

        assertTrue("Some runs must report a retry while the network fails", firstRuns.any { !it })
        assertTrue(outbox.local.values.all { it == SyncStatus.SYNCED })
        assertEquals(100, outbox.server.size)
    }
}
