package com.example.smartinventorypos.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * An amount of money held as whole minor units (poisha), so totals never pick up floating-point error.
 * Convert to and from Double only at the edges: the API and the existing Room columns.
 */
@JvmInline
value class Money private constructor(val minorUnits: Long) : Comparable<Money> {

    operator fun plus(other: Money) = Money(Math.addExact(minorUnits, other.minorUnits))
    operator fun minus(other: Money) = Money(Math.subtractExact(minorUnits, other.minorUnits))
    operator fun times(quantity: Int) = Money(Math.multiplyExact(minorUnits, quantity.toLong()))
    override fun compareTo(other: Money) = minorUnits.compareTo(other.minorUnits)

    val isPositive: Boolean get() = minorUnits > 0

    fun coerceAtLeastZero(): Money = if (minorUnits < 0) ZERO else this
    fun toBigDecimal(): BigDecimal = BigDecimal.valueOf(minorUnits, SCALE)
    fun toDouble(): Double = toBigDecimal().toDouble()
    fun toPlainString(): String = toBigDecimal().toPlainString()

    companion object {
        private const val SCALE = 2
        private val MAX_MAJOR = BigDecimal("1000000000000000")

        val ZERO = Money(0)

        fun ofMinor(minorUnits: Long) = Money(minorUnits)

        fun fromDouble(amount: Double): Money = fromBigDecimal(BigDecimal.valueOf(amount))

        /** Reads user input. Returns null if it isn't a number or is too large to hold. */
        fun parse(text: String): Money? {
            val value = text.trim().toBigDecimalOrNull() ?: return null
            return if (value.abs() < MAX_MAJOR) fromBigDecimal(value) else null
        }

        private fun fromBigDecimal(value: BigDecimal) =
            Money(value.setScale(SCALE, RoundingMode.HALF_UP).unscaledValue().longValueExact())
    }
}

fun Iterable<Money>.sum(): Money = fold(Money.ZERO) { total, amount -> total + amount }
