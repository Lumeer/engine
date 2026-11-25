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

/*
 * The original library runs on JavaScript and its behaviour depends on JS coercion rules
 * (String(x), Number(x), truthiness, String.prototype.trim, ...). These helpers reproduce
 * those rules so the JVM and JS builds give the same answers.
 */

/** Characters matched by the JS whitespace class and removed by String.prototype.trim. */
internal const val JS_WHITESPACE = "\t\n\u000B\u000C\r \u00A0\u1680\u2000\u2001\u2002\u2003\u2004\u2005\u2006\u2007\u2008\u2009\u200A\u2028\u2029\u202F\u205F\u3000\uFEFF"

/** Character class with the JS whitespace characters */
internal const val JS_S = "[${JS_WHITESPACE}]"

internal fun Char.isJsWhitespace(): Boolean = JS_WHITESPACE.indexOf(this) >= 0

internal fun jsTrim(value: String): String = value.trim { it.isJsWhitespace() }

internal fun removeJsWhitespace(value: String): String = value.filterNot { it.isJsWhitespace() }

/** Number -> string exactly like JS String(number). */
internal expect fun numberToJsString(value: Double): String

/** Unicode NFD normalization. */
internal expect fun normalizeNfd(value: String): String

/** String(value) for values that are not strings, numbers, booleans or arrays. */
internal expect fun objectToJsString(value: Any): String

/** JS String(value). Null gives "null" as in JS. */
internal fun jsString(value: Any?): String = when (value) {
    null -> "null"
    is String -> value
    is Boolean -> value.toString()
    is Double -> numberToJsString(value)
    is Float -> numberToJsString(value.toDouble())
    is Int, is Long, is Short, is Byte -> value.toString()
    is Number -> numberToJsString(value.toDouble())
    is Array<*> -> value.joinToString(",") { if (it == null) "" else jsString(it) }
    is Iterable<*> -> value.joinToString(",") { if (it == null) "" else jsString(it) }
    else -> objectToJsString(value)
}

/** JS toFixed(digits) */
internal expect fun jsToFixed(value: Double, digits: Int): String

/** The value as a list when it is a JS array (List or Array on the JVM), otherwise null. */
internal fun toJsList(value: Any?): List<Any?>? = when (value) {
    is List<*> -> value
    is Array<*> -> value.asList()
    else -> null
}

/** `x ? String(x) : ""` */
internal fun jsStringOrEmpty(value: Any?): String = if (jsTruthy(value)) jsString(value) else ""

/** `x || x === 0`: values the original library formats with String(x) */
internal fun hasJsDisplayValue(value: Any?): Boolean = jsTruthy(value) || (value is Number && value.toDouble() == 0.0)

/** A value or each value of an array, without null and blank ones */
internal fun nonBlankJsValues(value: Any?): List<Any?> =
    (toJsList(value) ?: listOf(value)).filter { it != null && jsTrim(jsString(it)).isNotEmpty() }

/** JS truthiness. */
internal fun jsTruthy(value: Any?): Boolean = when (value) {
    null -> false
    is Boolean -> value
    is Number -> value.toDouble().let { it != 0.0 && !it.isNaN() }
    is String -> value.isNotEmpty()
    else -> true
}

/** JS === for primitive values. */
internal fun jsStrictEquals(a: Any?, b: Any?): Boolean = when {
    a == null || b == null -> a == null && b == null
    a is Number && b is Number -> a.toDouble() == b.toDouble()
    a is String && b is String -> a == b
    a is Boolean && b is Boolean -> a == b
    else -> a === b
}

private val DECIMAL_LITERAL = Regex("^[+-]?(\\d+\\.?\\d*|\\.\\d+)([eE][+-]?\\d+)?$")

/** JS Number(string). */
internal fun stringToJsNumber(value: String): Double {
    val text = jsTrim(value)
    if (text.isEmpty()) return 0.0
    when (text) {
        "Infinity", "+Infinity" -> return Double.POSITIVE_INFINITY
        "-Infinity" -> return Double.NEGATIVE_INFINITY
    }
    if (text.length > 2 && text[0] == '0') {
        val radix = when (text[1]) {
            'x', 'X' -> 16
            'o', 'O' -> 8
            'b', 'B' -> 2
            else -> 0
        }
        if (radix > 0) {
            var result = 0.0
            for (c in text.substring(2)) {
                val digit = c.digitToIntOrNull(radix) ?: return Double.NaN
                result = result * radix + digit
            }
            return result
        }
    }
    if (DECIMAL_LITERAL.matches(text)) {
        return text.toDouble()
    }
    return Double.NaN
}

/** JS Number(value) for the value types that occur in documents. */
internal fun jsToNumber(value: Any?): Double = when (value) {
    null -> 0.0
    is Number -> value.toDouble()
    is Boolean -> if (value) 1.0 else 0.0
    is String -> stringToJsNumber(value)
    else -> stringToJsNumber(jsString(value))
}

/** @lumeer/utils toNumber */
internal fun toNumber(value: Any?): Double {
    if (value is Number) return value.toDouble()
    if (!jsTruthy(value)) return jsToNumber(value)
    return stringToJsNumber(removeJsWhitespace(jsString(value)).replaceFirst(",", "."))
}

/** @lumeer/utils isNumeric */
internal fun isNumeric(value: Any?): Boolean {
    if (value == null || value is Boolean || jsTrim(jsString(value)).isEmpty()) {
        return false
    }
    return !toNumber(value).isNaN()
}
