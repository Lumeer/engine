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

/** Arithmetic on non-negative integers written as decimal digit strings. */
internal object Digits {

    private fun strip(value: String): String = value.trimStart('0').ifEmpty { "0" }

    fun compare(a: String, b: String): Int {
        val x = strip(a)
        val y = strip(b)
        if (x.length != y.length) return x.length.compareTo(y.length)
        return x.compareTo(y)
    }

    fun add(a: String, b: String): String {
        val sb = StringBuilder()
        var i = a.length - 1
        var j = b.length - 1
        var carry = 0
        while (i >= 0 || j >= 0 || carry > 0) {
            val sum = (if (i >= 0) a[i--] - '0' else 0) + (if (j >= 0) b[j--] - '0' else 0) + carry
            sb.append('0' + sum % 10)
            carry = sum / 10
        }
        return strip(sb.reverse().toString())
    }

    /** a - b, requires a >= b */
    fun subtract(a: String, b: String): String {
        val sb = StringBuilder()
        var i = a.length - 1
        var j = b.length - 1
        var borrow = 0
        while (i >= 0) {
            var diff = (a[i--] - '0') - (if (j >= 0) b[j--] - '0' else 0) - borrow
            if (diff < 0) {
                diff += 10
                borrow = 1
            } else {
                borrow = 0
            }
            sb.append('0' + diff)
        }
        return strip(sb.reverse().toString())
    }

    fun multiply(a: String, b: String): String {
        val result = IntArray(a.length + b.length)
        for (i in a.indices.reversed()) {
            for (j in b.indices.reversed()) {
                val sum = (a[i] - '0') * (b[j] - '0') + result[i + j + 1]
                result[i + j + 1] = sum % 10
                result[i + j] += sum / 10
            }
        }
        return strip(result.joinToString(""))
    }

    /** floor(a / b) */
    fun divide(a: String, b: String): String {
        val divisor = strip(b)
        val quotient = StringBuilder()
        var remainder = "0"
        for (c in a) {
            remainder = strip(remainder + c)
            var digit = 0
            while (compare(remainder, divisor) >= 0) {
                remainder = subtract(remainder, divisor)
                digit++
            }
            quotient.append('0' + digit)
        }
        return strip(quotient.toString())
    }
}
