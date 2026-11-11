package smart_inventory_backend

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import smart_inventory_backend.common.TimeConfig
import smart_inventory_backend.sale.ReportService
import smart_inventory_backend.sale.SaleRepository
import java.lang.reflect.Proxy
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/** "Today" must be the shop's calendar day, even when the server's own zone is UTC. */
class ReportTimeZoneTest {

    /** A SaleRepository that only answers totalBetween, and remembers the range it was asked for. */
    private class RecordingSales {
        var from: Instant? = null
        var to: Instant? = null
        val repository: SaleRepository = Proxy.newProxyInstance(
            SaleRepository::class.java.classLoader, arrayOf(SaleRepository::class.java)
        ) { _, method, args ->
            check(method.name == "totalBetween") { "Unexpected call: ${method.name}" }
            from = args[0] as Instant
            to = args[1] as Instant
            BigDecimal.ZERO
        } as SaleRepository
    }

    @Test
    fun `the clock defaults to the shop's time zone`() {
        assertEquals(ZoneId.of("Asia/Dhaka"), TimeConfig().clock("Asia/Dhaka").zone)
    }

    @Test
    fun `today runs from midnight to midnight in Dhaka, not in UTC`() {
        // 19:00 UTC on 10 Nov is already 01:00 on 11 Nov in Dhaka (UTC+6).
        val now = Instant.parse("2026-11-10T19:00:00Z")
        val sales = RecordingSales()

        ReportService(sales.repository, Clock.fixed(now, ZoneId.of("Asia/Dhaka"))).todayTotal()

        assertEquals(Instant.parse("2026-11-10T18:00:00Z"), sales.from) // 00:00 on 11 Nov in Dhaka
        assertEquals(Instant.parse("2026-11-11T18:00:00Z"), sales.to)   // 00:00 on 12 Nov in Dhaka
    }

    @Test
    fun `a UTC clock would have picked the wrong day`() {
        val now = Instant.parse("2026-11-10T19:00:00Z")
        val sales = RecordingSales()

        ReportService(sales.repository, Clock.fixed(now, ZoneId.of("UTC"))).todayTotal()

        // Still 10 Nov in UTC: this is the bug the configured zone avoids.
        assertEquals(Instant.parse("2026-11-10T00:00:00Z"), sales.from)
    }
}
