/*
 * Lumeer: Modern Data Definition and Processing Platform
 *
 * Copyright (C) since 2017 Lumeer.io, s.r.o. and/or its affiliates.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.lumeer.datafilters.number

/**
 * Minimal arbitrary-precision decimal (replacement for big.js).
 * value = (-1)^negative * digits * 10^exponent; digits has no leading or trailing zeros ("0" for zero).
 */
class Decimal private constructor(
    private val negative: Boolean,
    private val digits: String,
    private val exponent: Int,
) : Comparable<Decimal> {

    val isZero: Boolean get() = digits == "0"

    val signum: Int get() = if (isZero) 0 else if (negative) -1 else 1

    override fun compareTo(other: Decimal): Int {
        if (isZero || other.isZero) {
            return signum.compareTo(other.signum)
        }
        if (negative != other.negative) {
            return if (negative) -1 else 1
        }
        val magnitude = compareMagnitude(other)
        return if (negative) -magnitude else magnitude
    }

    private fun compareMagnitude(other: Decimal): Int {
        val position = digits.length + exponent
        val otherPosition = other.digits.length + other.exponent
        if (position != otherPosition) {
            return position.compareTo(otherPosition)
        }
        val length = maxOf(digits.length, other.digits.length)
        return digits.padEnd(length, '0').compareTo(other.digits.padEnd(length, '0'))
    }

    fun abs(): Decimal = if (negative) Decimal(false, digits, exponent) else this

    fun negate(): Decimal = if (isZero) this else Decimal(!negative, digits, exponent)

    /** Rounds to [decimalPlaces] fractional digits, half away from zero (big.js Big.roundHalfUp). */
    fun roundHalfUp(decimalPlaces: Int): Decimal = round(decimalPlaces) { _, nextDigit -> nextDigit >= '5' }

    /** Rounds to [decimalPlaces] fractional digits, half towards positive infinity (numbro formatting). */
    fun roundHalfCeiling(decimalPlaces: Int): Decimal = round(decimalPlaces) { rest, nextDigit ->
        if (negative) nextDigit > '5' || (nextDigit == '5' && rest.any { it != '0' }) else nextDigit >= '5'
    }

    private fun round(decimalPlaces: Int, roundUp: (rest: String, nextDigit: Char) -> Boolean): Decimal {
        if (-exponent <= decimalPlaces) {
            return this
        }
        val keep = digits.length + exponent + decimalPlaces
        if (keep < 0) {
            return ZERO
        }
        val kept = digits.substring(0, keep)
        val next = digits[keep]
        val rest = digits.substring(keep + 1)
        val magnitude = if (roundUp(rest, next)) increment(kept) else kept
        return create(negative, magnitude.ifEmpty { "0" }, -decimalPlaces)
    }

    /** Plain notation without exponent (big.js toFixed()). */
    fun toPlainString(): String {
        val point = digits.length + exponent
        val body = when {
            isZero -> "0"
            exponent >= 0 -> digits + "0".repeat(exponent)
            point > 0 -> digits.substring(0, point) + "." + digits.substring(point)
            else -> "0." + "0".repeat(-point) + digits
        }
        return if (negative && !isZero) "-$body" else body
    }

    /** Plain notation with exactly [decimalPlaces] fractional digits; the value must already be rounded. */
    fun toFixedString(decimalPlaces: Int): String {
        val plain = abs().toPlainString()
        val dot = plain.indexOf('.')
        val integerPart = if (dot >= 0) plain.substring(0, dot) else plain
        val fraction = if (dot >= 0) plain.substring(dot + 1) else ""
        val body = if (decimalPlaces > 0) integerPart + "." + fraction.padEnd(decimalPlaces, '0') else integerPart
        return if (negative && !isZero) "-$body" else body
    }

    // ---- arithmetic (big.js semantics)

    operator fun plus(other: Decimal): Decimal {
        if (isZero) return other
        if (other.isZero) return this
        val e = minOf(exponent, other.exponent)
        val a = digits + "0".repeat(exponent - e)
        val b = other.digits + "0".repeat(other.exponent - e)
        return if (negative == other.negative) {
            create(negative, Digits.add(a, b), e)
        } else {
            val cmp = Digits.compare(a, b)
            when {
                cmp == 0 -> ZERO
                cmp > 0 -> create(negative, Digits.subtract(a, b), e)
                else -> create(other.negative, Digits.subtract(b, a), e)
            }
        }
    }

    operator fun minus(other: Decimal): Decimal = plus(other.negate())

    operator fun times(other: Decimal): Decimal {
        if (isZero || other.isZero) return ZERO
        return create(negative != other.negative, Digits.multiply(digits, other.digits), exponent + other.exponent)
    }

    /** big.js div with DP = 20 and ROUND_HALF_UP; null when dividing by zero */
    fun div(other: Decimal, decimalPlaces: Int = 20): Decimal? {
        if (other.isZero) return null
        if (isZero) return ZERO
        val k = exponent - other.exponent + decimalPlaces + 1
        val numerator = if (k >= 0) digits + "0".repeat(k) else digits
        val denominator = if (k >= 0) other.digits else other.digits + "0".repeat(-k)
        val quotient = Digits.divide(numerator, denominator)
        return create(negative != other.negative, quotient, -(decimalPlaces + 1)).roundHalfUp(decimalPlaces)
    }

    /** Moves the decimal point (big.e += n) */
    fun shift(places: Int): Decimal = if (isZero) this else Decimal(negative, digits, exponent + places)

    /** Rounds towards zero */
    fun roundDown(decimalPlaces: Int): Decimal = round(decimalPlaces) { _, _ -> false }

    /** big.js toFixed(dp) */
    fun toFixed(decimalPlaces: Int): String = roundHalfUp(decimalPlaces).toFixedString(decimalPlaces)

    /** big.js toString: exponential notation when the exponent is <= -7 or >= 21 */
    fun toBigJsString(): String {
        val e = digits.length + exponent - 1
        if (isZero || e in -6..20) return toPlainString()
        val mantissa = if (digits.length > 1) digits[0] + "." + digits.substring(1) else digits
        return (if (negative) "-" else "") + mantissa + "e" + (if (e < 0) "" else "+") + e
    }

    fun toDouble(): Double = toPlainString().toDouble()

    override fun equals(other: Any?): Boolean = other is Decimal && compareTo(other) == 0

    override fun hashCode(): Int = if (isZero) 0 else (digits.hashCode() * 31 + exponent) * (if (negative) -1 else 1)

    override fun toString(): String = toPlainString()

    companion object {
        val ZERO = Decimal(false, "0", 0)

        fun of(value: Long): Decimal = parse(value.toString())!!

        private val PATTERN = Regex("^(-?)(\\d+(?:\\.\\d*)?|\\.\\d+)(?:[eE]([+-]?\\d+))?$")

        /** Parses a string the same way as big.js `new Big(string)`; returns null where big.js throws. */
        fun parse(value: String): Decimal? {
            val match = PATTERN.find(value) ?: return null
            val sign = match.groupValues[1]
            val number = match.groupValues[2]
            val exponentPart = match.groupValues[3]
            val dot = number.indexOf('.')
            val integerPart = if (dot >= 0) number.substring(0, dot) else number
            val fraction = if (dot >= 0) number.substring(dot + 1) else ""
            val parsedExponent = if (exponentPart.isEmpty()) 0 else (exponentPart.removePrefix("+").toIntOrNull() ?: return null)
            return create(sign == "-", integerPart + fraction, parsedExponent - fraction.length)
        }

        private fun create(negative: Boolean, rawDigits: String, rawExponent: Int): Decimal {
            val digits = rawDigits.trimStart('0')
            if (digits.isEmpty()) {
                return ZERO
            }
            val trimmed = digits.trimEnd('0')
            return Decimal(negative, trimmed, rawExponent + digits.length - trimmed.length)
        }

        private fun increment(value: String): String {
            val chars = value.toCharArray()
            var i = chars.size - 1
            while (i >= 0) {
                if (chars[i] == '9') {
                    chars[i] = '0'
                    i--
                } else {
                    chars[i] = chars[i] + 1
                    return chars.concatToString()
                }
            }
            return "1" + chars.concatToString()
        }
    }
}
