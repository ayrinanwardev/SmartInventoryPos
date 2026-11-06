package com.example.smartinventorypos.domain

import com.example.smartinventorypos.domain.common.AppResult
import com.example.smartinventorypos.domain.common.DomainError
import com.example.smartinventorypos.domain.model.Cart
import com.example.smartinventorypos.domain.model.Money
import com.example.smartinventorypos.domain.model.PendingSale
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeSessions(role: Role?) : SessionStore {
    override val session: StateFlow<UserSession?> = MutableStateFlow(UserSession("token", role))
    override fun save(session: UserSession) = Unit
    override fun clear() = Unit
    override fun clearIfCurrent(token: String) = Unit
}

private class FakeSales(
    private val uploads: Map<String, AppResult<Unit>> = emptyMap(),
    private val recordResult: AppResult<Unit> = AppResult.Success(Unit)
) : OfflineSaleRepository {
    val recordedDiscounts = mutableListOf<Money>()
    val statuses = mutableMapOf<String, SyncStatus>()

    override fun pendingCount(): Flow<Int> = emptyFlow()
    override fun rejectedCount(): Flow<Int> = flowOf(0)
    override suspend fun hasPending() = statuses.isEmpty() && uploads.isNotEmpty()
    override suspend fun acknowledgeRejected() = Unit
    override suspend fun record(cart: Cart, discount: Money): AppResult<Unit> {
        recordedDiscounts += discount
        return recordResult
    }
    override suspend fun pendingSales() = uploads.keys.map { PendingSale(it, emptyList(), Money.ZERO) }
    override suspend fun upload(sale: PendingSale) = uploads.getValue(sale.clientId)
    override suspend fun markStatus(clientId: String, status: SyncStatus) {
        statuses[clientId] = status
    }
}

private class FakeCatalog : ProductCatalogRepository {
    var refreshes = 0
    override fun search(query: String): Flow<List<Product>> = emptyFlow()
    override suspend fun refreshFromServer(): AppResult<Unit> {
        refreshes++
        return AppResult.Success(Unit)
    }
}

private class FakeScheduler : SaleSyncScheduler {
    var scheduled = 0
    override fun schedule() {
        scheduled++
    }
}

class CheckoutUseCaseTest {
    private val cart = Cart().add(Product(1, "Tea", null, Money.ofMinor(1000), stock = 5))
    private val sales = FakeSales()
    private val scheduler = FakeScheduler()

    private fun checkout(role: Role, sales: FakeSales = this.sales) =
        CheckoutUseCase(FakeSessions(role), sales, scheduler)

    @Test
    fun `a valid sale is saved and then scheduled`() = runTest {
        val result = checkout(Role.CASHIER)(cart, Money.ofMinor(100))
        assertEquals(AppResult.Success(Unit), result)
        assertEquals(listOf(Money.ofMinor(100)), sales.recordedDiscounts)
        assertEquals(1, scheduler.scheduled)
    }

    @Test
    fun `a customer's discount is ignored`() = runTest {
        checkout(Role.CUSTOMER)(cart, Money.ofMinor(500))
        assertEquals(listOf(Money.ZERO), sales.recordedDiscounts)
    }

    @Test
    fun `a discount above the subtotal is rejected and nothing is saved`() = runTest {
        val result = checkout(Role.MANAGER)(cart, Money.ofMinor(1001))
        assertEquals(AppResult.Failure(DomainError.InvalidDiscount), result)
        assertTrue(sales.recordedDiscounts.isEmpty())
        assertEquals(0, scheduler.scheduled)
    }

    @Test
    fun `a negative discount is rejected`() = runTest {
        val result = checkout(Role.MANAGER)(cart, Money.ofMinor(-1))
        assertEquals(AppResult.Failure(DomainError.InvalidDiscount), result)
    }

    @Test
    fun `an empty cart is rejected`() = runTest {
        assertEquals(AppResult.Failure(DomainError.EmptyCart), checkout(Role.CASHIER)(Cart(), Money.ZERO))
    }

    @Test
    fun `no sync is scheduled when saving fails`() = runTest {
        val failing = FakeSales(recordResult = AppResult.Failure(DomainError.Unknown))
        checkout(Role.CASHIER, failing)(cart, Money.ZERO)
        assertEquals(0, scheduler.scheduled)
    }
}

class SyncPendingSalesUseCaseTest {
    private val catalog = FakeCatalog()

    private fun sync(sales: FakeSales): SyncPendingSalesUseCase {
        val refresh = RefreshCatalogUseCase(FakeSessions(Role.CASHIER), sales, catalog, FakeScheduler())
        return SyncPendingSalesUseCase(sales, refresh)
    }

    @Test
    fun `accepted sales are marked synced and the catalog refreshes`() = runTest {
        val sales = FakeSales(mapOf("a" to AppResult.Success(Unit)))
        assertTrue(sync(sales)())
        assertEquals(SyncStatus.SYNCED, sales.statuses["a"])
        assertEquals(1, catalog.refreshes)
    }

    @Test
    fun `permanent rejections go to review without a retry`() = runTest {
        for (error in listOf(DomainError.BadRequest, DomainError.NotFound, DomainError.Conflict)) {
            val sales = FakeSales(mapOf("a" to AppResult.Failure(error)))
            assertTrue(sync(sales)())
            assertEquals(SyncStatus.REJECTED, sales.statuses["a"])
        }
    }

    @Test
    fun `temporary failures stay pending and ask for a retry`() = runTest {
        for (error in listOf(DomainError.Network, DomainError.Unauthorized, DomainError.Server(500), DomainError.Unknown)) {
            val sales = FakeSales(mapOf("a" to AppResult.Failure(error)))
            assertFalse(sync(sales)())
            assertTrue(sales.statuses.isEmpty())
        }
        assertEquals(0, catalog.refreshes)
    }

    @Test
    fun `one failing sale doesn't stop the others`() = runTest {
        val sales = FakeSales(
            mapOf("a" to AppResult.Failure(DomainError.Network), "b" to AppResult.Success(Unit))
        )
        assertFalse(sync(sales)())
        assertEquals(SyncStatus.SYNCED, sales.statuses["b"])
    }
}

class RefreshCatalogUseCaseTest {
    @Test
    fun `pending sales are sent first instead of overwriting local stock`() = runTest {
        val catalog = FakeCatalog()
        val scheduler = FakeScheduler()
        val sales = FakeSales(mapOf("a" to AppResult.Success(Unit)))
        RefreshCatalogUseCase(FakeSessions(Role.CASHIER), sales, catalog, scheduler)()
        assertEquals(0, catalog.refreshes)
        assertEquals(1, scheduler.scheduled)
    }

    @Test
    fun `with nothing pending the catalog refreshes`() = runTest {
        val catalog = FakeCatalog()
        RefreshCatalogUseCase(FakeSessions(Role.CASHIER), FakeSales(), catalog, FakeScheduler())()
        assertEquals(1, catalog.refreshes)
    }
}
