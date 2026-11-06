package smart_inventory_backend

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.UUID

/**
 * Sends real HTTP requests to a running server. MockMvc would not do here: it skips Spring's
 * error dispatch, which is where security can turn a 409 into a 401.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "spring.datasource.url=jdbc:h2:mem:apitest;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.username=sa",
        "spring.datasource.password="
    ]
)
@ActiveProfiles("dev")
class ApiIntegrationTest {

    @LocalServerPort private var port = 0
    @Autowired private lateinit var mapper: ObjectMapper

    private val http = HttpClient.newHttpClient()

    private data class Res(val status: Int, val body: String)

    private fun call(method: String, path: String, token: String? = null, json: String? = null): Res {
        val b = HttpRequest.newBuilder(URI("http://localhost:$port$path"))
            .header("Content-Type", "application/json")
            .method(
                method,
                json?.let { HttpRequest.BodyPublishers.ofString(it) } ?: HttpRequest.BodyPublishers.noBody()
            )
        token?.let { b.header("Authorization", "Bearer $it") }
        val r = http.send(b.build(), HttpResponse.BodyHandlers.ofString())
        return Res(r.statusCode(), r.body())
    }

    private fun tree(r: Res): JsonNode = mapper.readTree(r.body)

    private fun login(user: String, pass: String): String {
        val r = call("POST", "/api/auth/login", json = """{"username":"$user","password":"$pass"}""")
        assertEquals(200, r.status)
        return tree(r)["token"].asText()
    }

    private fun manager() = login("manager", "manager123")
    private fun cashier() = login("cashier", "cashier123")

    private fun unique() = UUID.randomUUID().toString().take(8)

    private fun newProduct(stock: Int = 10, price: String = "50.50"): Long {
        val r = call(
            "POST", "/api/products", manager(),
            """{"name":"P-${unique()}","barcode":"${unique()}","price":$price,"stock":$stock}"""
        )
        assertEquals(201, r.status)
        return tree(r)["id"].asLong()
    }

    private fun saleJson(productId: Long, qty: Int, discount: String = "0", clientId: UUID = UUID.randomUUID()) =
        """{"clientId":"$clientId","items":[{"productId":$productId,"qty":$qty}],"discount":$discount}"""

    private fun stockOf(id: Long): Int =
        tree(call("GET", "/api/products", manager())).first { it["id"].asLong() == id }["stock"].asInt()

    // ----- auth -----
    @Test fun `wrong password is 401`() =
        assertEquals(401, call("POST", "/api/auth/login", json = """{"username":"manager","password":"nope"}""").status)

    @Test fun `request without a token is 401`() =
        assertEquals(401, call("GET", "/api/products").status)

    @Test fun `invalid token is 401`() =
        assertEquals(401, call("GET", "/api/products", "not-a-token").status)

    @Test fun `login returns token and role`() {
        val r = call("POST", "/api/auth/login", json = """{"username":"cashier","password":"cashier123"}""")
        assertEquals(200, r.status)
        assertEquals("CASHIER", tree(r)["role"].asText())
        assertTrue(tree(r)["token"].asText().isNotBlank())
    }

    @Test fun `register creates a customer`() {
        val r = call("POST", "/api/auth/register", json = """{"username":"u${unique()}","password":"secret12"}""")
        assertEquals(201, r.status)
        assertEquals("CUSTOMER", tree(r)["role"].asText())
    }

    @Test fun `duplicate username is 409`() =
        assertEquals(409, call("POST", "/api/auth/register", json = """{"username":"manager","password":"secret12"}""").status)

    @Test fun `too short registration is 400`() =
        assertEquals(400, call("POST", "/api/auth/register", json = """{"username":"ab","password":"1"}""").status)

    // ----- roles -----
    @Test fun `cashier cannot create a product`() =
        assertEquals(403, call("POST", "/api/products", cashier(), """{"name":"X","price":1,"stock":1}""").status)

    @Test fun `cashier cannot see sales reports`() {
        assertEquals(403, call("GET", "/api/sales", cashier()).status)
        assertEquals(403, call("GET", "/api/sales/report/today", cashier()).status)
    }

    @Test fun `cashier stock change is limited to 20`() {
        val id = newProduct()
        assertEquals(403, call("POST", "/api/products/$id/stock", cashier(), """{"delta":21,"reason":"restock"}""").status)
        val ok = call("POST", "/api/products/$id/stock", cashier(), """{"delta":20,"reason":"restock"}""")
        assertEquals(200, ok.status)
        assertEquals(30, tree(ok)["stock"].asInt())
    }

    @Test fun `stock cannot go below zero`() {
        val id = newProduct(stock = 1)
        assertEquals(409, call("POST", "/api/products/$id/stock", manager(), """{"delta":-5,"reason":"damaged"}""").status)
    }

    // ----- sales -----
    @Test fun `sale reduces stock and calculates total on the server`() {
        val id = newProduct(stock = 10, price = "50.50")
        val r = call("POST", "/api/sales", manager(), saleJson(id, 2, "1.50"))
        assertEquals(201, r.status)
        assertEquals(99.5, tree(r)["total"].asDouble())
        assertEquals(8, stockOf(id))
    }

    @Test fun `resending the same sale does not sell twice`() {
        val id = newProduct(stock = 10)
        val clientId = UUID.randomUUID()
        val first = call("POST", "/api/sales", cashier(), saleJson(id, 3, clientId = clientId))
        val second = call("POST", "/api/sales", cashier(), saleJson(id, 3, clientId = clientId))
        assertEquals(tree(first)["id"].asLong(), tree(second)["id"].asLong())
        assertEquals(7, stockOf(id))
    }

    @Test fun `insufficient stock is 409 and changes nothing`() {
        val id = newProduct(stock = 2)
        assertEquals(409, call("POST", "/api/sales", cashier(), saleJson(id, 3)).status)
        assertEquals(2, stockOf(id))
    }

    @Test fun `unknown product is 404`() =
        assertEquals(404, call("POST", "/api/sales", cashier(), saleJson(999_999, 1)).status)

    @Test fun `discount above subtotal is 400 and stock is rolled back`() {
        val id = newProduct(stock = 5, price = "10")
        assertEquals(400, call("POST", "/api/sales", cashier(), saleJson(id, 1, "999")).status)
        assertEquals(5, stockOf(id))
    }

    @Test fun `customer discount is ignored`() {
        val id = newProduct(stock = 5, price = "10")
        val token = tree(
            call("POST", "/api/auth/register", json = """{"username":"c${unique()}","password":"secret12"}""")
        )["token"].asText()
        val r = call("POST", "/api/sales", token, saleJson(id, 1, "5"))
        assertEquals(201, r.status)
        assertEquals(10.0, tree(r)["total"].asDouble())
    }

    // ----- deleted products -----
    @Test fun `deleted product disappears and cannot be sold`() {
        val id = newProduct()
        assertEquals(204, call("DELETE", "/api/products/$id", manager()).status)
        assertFalse(tree(call("GET", "/api/products", manager())).any { it["id"].asLong() == id })
        assertEquals(409, call("POST", "/api/sales", cashier(), saleJson(id, 1)).status)
    }

    @Test fun `re-adding a deleted product brings it back`() {
        val name = "Back-${unique()}"
        val body = """{"name":"$name","barcode":null,"price":5,"stock":1}"""
        val id = tree(call("POST", "/api/products", manager(), body))["id"].asLong()
        call("DELETE", "/api/products/$id", manager())
        assertEquals(id, tree(call("POST", "/api/products", manager(), body))["id"].asLong())
        assertTrue(tree(call("GET", "/api/products", manager())).any { it["id"].asLong() == id })
    }

    // ----- response shape the Android app parses -----
    @Test fun `sale history has the fields the app reads and no password`() {
        val id = newProduct()
        val token = manager()
        assertEquals(201, call("POST", "/api/sales", token, saleJson(id, 1)).status)
        val raw = call("GET", "/api/sales/mine", token)
        assertEquals(200, raw.status)
        assertFalse(raw.body.contains("passwordHash"))
        val sale = tree(raw).first { s -> s["items"].any { it["product"]["id"].asLong() == id } }
        assertTrue(sale["clientId"].isTextual)
        assertTrue(sale["createdAt"].isTextual)
        assertEquals("manager", sale["cashier"]["username"].asText())
        assertEquals(1, sale["items"][0]["qty"].asInt())
        assertTrue(sale["items"][0]["unitPrice"].isNumber)
    }

    @Test fun `today total is a plain number`() {
        val r = call("GET", "/api/sales/report/today", manager())
        assertEquals(200, r.status)
        assertTrue(tree(r).isNumber)
    }

    @Test fun `low stock list works`() {
        val id = newProduct(stock = 1)
        val r = call("GET", "/api/products/low-stock", cashier())
        assertEquals(200, r.status)
        assertTrue(tree(r).any { it["id"].asLong() == id })
    }

    @Test fun `product response has exactly the fields clients rely on`() {
        val r = call("GET", "/api/products", manager())
        newProduct()
        val product = tree(call("GET", "/api/products", manager())).first()
        assertEquals(200, r.status)
        assertEquals(
            listOf("id", "name", "barcode", "price", "stock", "lowStockThreshold", "active", "updatedAt"),
            product.fieldNames().asSequence().toList()
        )
    }

    @Test fun `sale history is a plain list with or without paging`() {
        val id = newProduct(stock = 10)
        repeat(3) { assertEquals(201, call("POST", "/api/sales", manager(), saleJson(id, 1)).status) }
        val token = manager()
        assertTrue(tree(call("GET", "/api/sales", token)).size() >= 3)
        val page = tree(call("GET", "/api/sales?page=0&size=2", token))
        assertTrue(page.isArray)
        assertEquals(2, page.size())
        val second = tree(call("GET", "/api/sales?page=1&size=2", token))
        assertFalse(page.any { a -> second.any { b -> a["id"].asLong() == b["id"].asLong() } })
    }

    @Test fun `stock adjustment is recorded with who made it`() {
        val id = newProduct()
        assertEquals(200, call("POST", "/api/products/$id/stock", cashier(), """{"delta":-2,"reason":" damaged "}""").status)
        val entry = tree(call("GET", "/api/stock/adjustments", manager())).first { it["product"]["id"].asLong() == id }
        assertEquals("cashier", entry["performedBy"].asText())
        assertEquals("CASHIER", entry["role"].asText())
        assertEquals("damaged", entry["reason"].asText())
        assertEquals(8, entry["stockAfter"].asInt())
    }

    @Test fun `updating to a barcode already in use is 409`() {
        val barcode = unique()
        val token = manager()
        assertEquals(201, call("POST", "/api/products", token, """{"name":"A-${unique()}","barcode":"$barcode","price":1,"stock":1}""").status)
        val other = newProduct()
        val r = call("PUT", "/api/products/$other", token, """{"name":"B-${unique()}","barcode":"$barcode","price":1,"stock":1}""")
        assertEquals(409, r.status)
        assertEquals("BARCODE_IN_USE", tree(r)["code"].asText())
    }

    @Test fun `domain errors use the shared format`() {
        val r = call("POST", "/api/sales", cashier(), saleJson(999_999, 1))
        assertEquals(404, r.status)
        assertEquals("PRODUCT_NOT_FOUND", tree(r)["code"].asText())
        assertTrue(tree(r)["message"].asText().isNotBlank())
    }

    @Test fun `validation errors list the fields`() {
        val r = call("POST", "/api/auth/register", json = """{"username":"ab","password":"1"}""")
        assertEquals(400, r.status)
        assertEquals("VALIDATION_FAILED", tree(r)["code"].asText())
        assertTrue(tree(r)["details"].size() >= 2)
    }

    @Test fun `malformed json is 400`() =
        assertEquals(400, call("POST", "/api/auth/login", json = "{not json").status)
}
