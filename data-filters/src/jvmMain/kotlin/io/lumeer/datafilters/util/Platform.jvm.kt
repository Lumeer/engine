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
package io.lumeer.datafilters.util

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.Normalizer

internal actual fun numberToJsString(value: Double): String {
    if (value.isNaN()) return "NaN"
    if (value.isInfinite()) return if (value > 0) "Infinity" else "-Infinity"
    if (value == 0.0) return "0"
    // integers below 2^53 print as plain digits in JS
    if (value == kotlin.math.floor(value) && kotlin.math.abs(value) < 9.007199254740992E15) return value.toLong().toString()
    val decimal = shortestDecimal(value)
    val digits = decimal.unscaledValue().abs().toString()
    val exponent = digits.length - decimal.scale() - 1 // value = d.ddd * 10^exponent
    val sign = if (value < 0) "-" else ""
    // ECMAScript Number::toString: plain notation for 1e-7 < |value| < 1e21
    return if (exponent in -6..20) {
        sign + decimal.abs().toPlainString()
    } else {
        val mantissa = if (digits.length > 1) digits[0] + "." + digits.substring(1) else digits
        sign + mantissa + "e" + (if (exponent >= 0) "+" else "-") + kotlin.math.abs(exponent)
    }
}

/** Shortest decimal that converts back to the same double, closest to the exact value (as ECMAScript requires). */
private fun shortestDecimal(value: Double): BigDecimal {
    val exact = BigDecimal(value)
    for (precision in 1..17) {
        val candidate = exact.round(MathContext(precision, RoundingMode.HALF_EVEN))
        if (candidate.toDouble() == value) {
            return candidate.stripTrailingZeros()
        }
    }
    return exact.stripTrailingZeros()
}

internal actual fun jsToFixed(value: Double, digits: Int): String {
    if (value.isNaN()) return "NaN"
    if (kotlin.math.abs(value) >= 1e21 || value.isInfinite()) return numberToJsString(value)
    val fixed = BigDecimal(kotlin.math.abs(value)).setScale(digits, RoundingMode.HALF_UP).toPlainString()
    return if (value < 0) "-$fixed" else fixed
}

internal actual fun normalizeNfd(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)

internal actual fun objectToJsString(value: Any): String = when (value) {
    is Map<*, *> -> "[object Object]"
    else -> value.toString()
}
