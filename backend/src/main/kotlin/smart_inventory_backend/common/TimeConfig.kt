package smart_inventory_backend.common

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock
import java.time.ZoneId

/**
 * The clock in the shop's time zone. "Today" in reports means the shop's calendar day, not the
 * server's: cloud hosts usually run in UTC, six hours behind Bangladesh.
 */
@Configuration
class TimeConfig {
    @Bean
    fun clock(@Value("\${app.time-zone:Asia/Dhaka}") timeZone: String): Clock = Clock.system(ZoneId.of(timeZone))
}
