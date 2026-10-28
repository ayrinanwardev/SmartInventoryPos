package smart_inventory_backend.web

import jakarta.validation.Valid
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import smart_inventory_backend.model.Product
import smart_inventory_backend.repo.ProductRepository

@RestController
@RequestMapping("/api/products")
class ProductController(private val repo: ProductRepository) {

    @GetMapping
    fun list(@RequestParam(required = false) q: String?): List<Product> {
        val products = if (q.isNullOrBlank()) {
            repo.findAll()
        } else {
            repo.findByNameContainingIgnoreCaseOrBarcode(q, q)
        }

        return products.filter { it.active }
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('MANAGER','CASHIER')")
    fun lowStock(): List<Product> =
        repo.findLowStock().filter { it.active }

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody input: Product): Product {
        val barcode = input.barcode?.trim()?.ifBlank { null }
        val name = input.name.trim()

        // barcode thakle barcode diye, na thakle naam diye age check
        val existing = if (barcode != null) repo.findByBarcode(barcode)
        else repo.findFirstByNameIgnoreCase(name)

        if (existing != null) {
            existing.stock += input.stock        // restock
            existing.price = input.price         // notun price
            return repo.save(existing)
        }

        input.id = null
        input.name = name
        input.barcode = barcode
        return repo.save(input)
    }



    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    fun update(@PathVariable id: Long, @Valid @RequestBody input: Product): Product {
        val product = repo.findById(id).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }
        product.name = input.name
        product.barcode = input.barcode?.trim()?.ifBlank { null }
        product.price = input.price
        product.stock = input.stock
        product.lowStockThreshold = input.lowStockThreshold
        try {
            return repo.save(product)
        } catch (e: DataIntegrityViolationException) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Barcode already used")
        }
    }


    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) {
        val product = repo.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND)
        }

        product.active = false
        repo.save(product)
    }
}
