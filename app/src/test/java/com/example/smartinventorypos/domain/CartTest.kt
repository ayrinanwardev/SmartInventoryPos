package com.example.smartinventorypos.domain

import com.example.smartinventorypos.domain.model.Cart
import com.example.smartinventorypos.domain.model.Money
import com.example.smartinventorypos.domain.model.Product
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CartTest {
    private val tea = Product(1, "Tea", null, Money.ofMinor(1050), stock = 2)
    private val soldOut = Product(2, "Rice", null, Money.ofMinor(5000), stock = 0)

    @Test
    fun `adding increases the quantity up to the stock limit`() {
        val cart = Cart().add(tea).add(tea).add(tea)
        assertEquals(2, cart.quantityOf(tea.id))
    }

    @Test
    fun `a sold out product can't be added`() {
        assertTrue(Cart().add(soldOut).isEmpty)
    }

    @Test
    fun `a line is removed when its quantity reaches zero`() {
        assertTrue(Cart().add(tea).changeQuantity(tea.id, -1).isEmpty)
    }

    @Test
    fun `totals cover every line`() {
        val coffee = Product(3, "Coffee", null, Money.ofMinor(200), stock = 9)
        val cart = Cart().add(tea).add(tea).add(coffee)
        assertEquals(3, cart.itemCount)
        assertEquals(Money.ofMinor(2300), cart.subtotal)
    }

    @Test
    fun `changing the cart leaves the original untouched`() {
        val original = Cart().add(tea)
        original.add(tea)
        assertEquals(1, original.quantityOf(tea.id))
    }
}
