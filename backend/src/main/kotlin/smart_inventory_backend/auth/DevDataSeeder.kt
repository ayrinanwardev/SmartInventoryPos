package smart_inventory_backend.auth

import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

/** Creates demo staff accounts on an empty database. Only with --spring.profiles.active=dev. */
@Component
@Profile("dev")
class DevDataSeeder(
    private val users: UserRepository,
    private val encoder: PasswordEncoder
) : CommandLineRunner {

    override fun run(vararg args: String) {
        if (users.count() > 0) return
        users.save(AppUser("manager", encoder.encode("manager123"), Role.MANAGER))
        users.save(AppUser("cashier", encoder.encode("cashier123"), Role.CASHIER))
    }
}
