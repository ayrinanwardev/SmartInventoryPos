package smart_inventory_backend

import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.security.crypto.password.PasswordEncoder
import smart_inventory_backend.model.AppUser
import smart_inventory_backend.repo.UserRepository


@SpringBootApplication
class SmartInventoryApplication {

	/** Creates two demo users on first start. */
	@Bean
	fun seed(users: UserRepository, enc: PasswordEncoder) = CommandLineRunner {
		if (users.count() == 0L) {
            enc.encode("manager123")?.let { username -> users.save(AppUser(username = "manager", passwordHash = username, role = "MANAGER")) }
            enc.encode("cashier123")?.let { username -> users.save(AppUser(username = "cashier", passwordHash = username, role = "CASHIER")) }
		}
	}
}

fun main(args: Array<String>) {
	runApplication<SmartInventoryApplication>(*args)
}
