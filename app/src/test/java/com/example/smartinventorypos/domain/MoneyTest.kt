package com.example.smartinventorypos.domain

import com.example.smartinventorypos.domain.model.Money
import com.example.smartinventorypos.domain.model.sum
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {
    @Test
    fun `adding amounts has no floating point error`() {
        // 0.1 + 0.2 as Doubles is 0.30000000000000004.
        assertEquals(Money.ofMinor(30), Money.fromDouble(0.1) + Money.fromDouble(0.2))
    }

    @Test
    fun `many lines add up exactly`() {
        val total = List(1000) { Money.fromDouble(19.99) }.sum()
        assertEquals("19990.00", total.toPlainString())
    }

    @Test
    fun `parse reads user input and rounds half up`() {
        assertEquals(Money.ofMinor(1250), Money.parse(" 12.5 "))
        assertEquals(Money.ofMinor(1235), Money.parse("12.345"))
        assertEquals(Money.ofMinor(-100), Money.parse("-1"))
    }

    @Test
    fun `parse rejects text that is not a usable amount`() {
        assertNull(Money.parse("abc"))
        assertNull(Money.parse(""))
        assertNull(Money.parse("NaN"))
        assertNull(Money.parse("1e30"))
    }

    @Test
    fun `converting to a double and back keeps the value`() {
        val amount = Money.ofMinor(123456)
        assertEquals(amount, Money.fromDouble(amount.toDouble()))
    }

    @Test
    fun `coerceAtLeastZero only changes negative amounts`() {
        assertEquals(Money.ZERO, Money.ofMinor(-5).coerceAtLeastZero())
        assertEquals(Money.ofMinor(5), Money.ofMinor(5).coerceAtLeastZero())
    }
}
