package smart_inventory_backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SmartInventoryApplication

fun main(args: Array<String>) {
    runApplication<SmartInventoryApplication>(*args)
}
