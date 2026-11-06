package smart_inventory_backend.product

import jakarta.validation.Valid
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

@RestController
@RequestMapping("/api/products")
class ProductController(private val products: ProductService) {

    @GetMapping
    fun list(@RequestParam(required = false) q: String?): List<ProductResponse> = products.list(q)

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('MANAGER','CASHIER')")
    fun lowStock(): List<ProductResponse> = products.lowStock()

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody req: ProductRequest): ProductResponse = products.createOrRestock(req)

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    fun update(@PathVariable id: Long, @Valid @RequestBody req: ProductRequest): ProductResponse =
        products.update(id, req)

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) = products.delete(id)
}
