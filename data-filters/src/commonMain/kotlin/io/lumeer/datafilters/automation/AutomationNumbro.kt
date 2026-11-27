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
package io.lumeer.datafilters.automation

import io.lumeer.datafilters.number.Numbro
import io.lumeer.datafilters.util.jsString
import io.lumeer.datafilters.util.numberToJsString
import io.lumeer.datafilters.util.stringToJsNumber
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow

/**
 * The currency formatting of numbro 2.5.0 with all its languages registered, as the automation block
 * "format currency" uses it: numbro.setLanguage(locale); numbro(value).formatCurrency({mantissa: decimals, thousandSeparated: true}).
 */
internal object AutomationNumbro {

    private val languages: Map<String, NumbroLanguage> = NUMBRO_LANGUAGES.associateBy { it.tag }

    /** state.setLanguage with the en-US fallback */
    fun language(tag: String): NumbroLanguage {
        languages[tag]?.let { return it }
        val prefix = tag.split("-")[0]
        val matching = NUMBRO_LANGUAGES.firstOrNull { it.tag.split("-")[0] == prefix }
        return matching ?: languages.getValue("en-US")
    }

    fun formatCurrency(tag: String, mantissa: Double, input: Any?): String {
        val language = language(tag)
        val value = normalizeInput(input, language)
        // validateFormat: mantissa must be a number >= 0
        if (!(mantissa >= 0)) return "ERROR: invalid format"
        val output = formatNumber(value, mantissa, language)
        val number = jsNumber(value)
        return when (language.currencyPosition) {
            "prefix" -> if (number < 0) "-" + language.currencySymbol + output.substring(1) else language.currencySymbol + output
            "infix" -> error("infix currencies are not used")
            else -> output + language.currencySymbol
        }
    }

    /** `value.toString()` (a TypeError for null and undefined) */
    private fun toStringOf(value: Any?): String = when (value) {
        null, JsUndefined -> throw AutomationFunctionException("Cannot read properties of $value (reading 'toString')")
        is JsDate -> value.string ?: throw AutomationFunctionException("Formatting a date as a currency is not supported")
        else -> jsString(value)
    }

    private fun normalizeInput(input: Any?, language: NumbroLanguage): Any? = when {
        input is String -> unformat(input, language)
        jsNumber(input).isNaN() -> Double.NaN
        else -> input
    }

    private fun formatNumber(value: Any?, mantissa: Double, language: NumbroLanguage): String {
        if (!jsNumber(value).isFinite()) {
            return toStringOf(value)
        }
        var output = setMantissaPrecision(value, mantissa)
        output = setCharacteristicPrecision(output, value)
        output = replaceDelimiters(output, value, language)
        if (jsNumber(value) < 0) {
            output = insertSign(output, value)
        }
        return output
    }

    private fun setMantissaPrecision(value: Any?, precision: Double): String = toFixed(value, precision)

    private fun toFixed(value: Any?, precision: Double): String {
        val string = toStringOf(value)
        if (string.indexOf("e") != -1) {
            return toFixedLarge(string, precision)
        }
        val rounded = ScriptDate.jsRound(stringToJsNumber(string + "e+" + numberToJsString(precision))) / jsPow(10.0, precision)
        return bigNumberToFixed(rounded, precision)
    }

    /** Math.pow of the script engine: integer exponents by squaring */
    private fun jsPow(base: Double, exponent: Double): Double {
        if (exponent != floor(exponent) || abs(exponent) > Int.MAX_VALUE) return base.pow(exponent)
        var result = 1.0
        var b = base
        var e = abs(exponent).toInt()
        while (e > 0) {
            if (e and 1 == 1) result *= b
            b *= b
            e = e shr 1
        }
        return if (exponent < 0) 1 / result else result
    }

    private fun zeroes(count: Double): String {
        if (count.isInfinite()) throw AutomationFunctionException("Endless loop")
        return buildString { var i = 0; while (i < count) { append('0'); i++ } }
    }

    private fun toFixedLarge(string: String, precision: Double): String {
        val e = string.indexOf("e")
        val base = string.substring(0, e)
        val rest = string.substring(e + 1)
        val exp = rest.substringBefore("e")
        val dot = base.indexOf(".")
        val characteristic = if (dot >= 0) base.substring(0, dot) else base
        val mantissa = if (dot >= 0) base.substring(dot + 1).substringBefore(".") else ""
        val expNumber = stringToJsNumber(exp)
        var result: String
        if (expNumber > 0) {
            result = characteristic + mantissa + zeroes(jsNumber(exp) - mantissa.length)
        } else {
            val prefix = if (stringToJsNumber(characteristic) < 0) "-0." else "0."
            val absCharacteristic = numberToJsString(abs(stringToJsNumber(characteristic)))
            var suffix = (zeroes(-expNumber - 1) + absCharacteristic + mantissa).let { s ->
                // substr(0, precision)
                val length = if (precision.isNaN()) 0 else precision.coerceIn(0.0, s.length.toDouble()).toInt()
                s.substring(0, length)
            }
            if (suffix.length < precision) {
                suffix += zeroes(precision - suffix.length)
            }
            result = prefix + suffix
        }
        if (expNumber > 0 && precision > 0) {
            result += "." + zeroes(precision)
        }
        return result
    }

    private fun setCharacteristicPrecision(output: String, value: Any?): String {
        // characteristic precision 0 and no optionalCharacteristic: only removes and adds back the sign
        return output
    }

    private fun replaceDelimiters(output: String, value: Any?, language: NumbroLanguage): String {
        val parts = output.split(".")
        var characteristic = parts[0]
        val mantissa = parts.getOrNull(1)
        val hasNegativeSign = jsNumber(value) < 0 && characteristic.indexOf("-") == 0
        if (hasNegativeSign) characteristic = characteristic.substring(1)
        val indexes = indexesOfGroupSpaces(characteristic.length, 3)
        indexes.forEachIndexed { index, position ->
            characteristic = characteristic.substring(0, position + index) + language.thousands + characteristic.substring(position + index)
        }
        if (hasNegativeSign) characteristic = "-$characteristic"
        return if (mantissa.isNullOrEmpty()) characteristic else characteristic + language.decimal + mantissa
    }

    private fun indexesOfGroupSpaces(totalLength: Int, groupSize: Int): List<Int> {
        val result = ArrayDeque<Int>()
        var counter = 0
        for (i in totalLength downTo 1) {
            if (counter == groupSize) {
                result.addFirst(i)
                counter = 0
            }
            counter++
        }
        return result
    }

    private fun insertSign(output: String, value: Any?): String {
        if (value is Number && value.toDouble() == 0.0) return output
        if (stringToJsNumber(output) == 0.0) return output.replaceFirst("-", "")
        return output
    }

    // ---- bignumber.js 9: new BigNumber(x).toFixed(dp) with ROUND_HALF_UP

    fun bigNumberToFixed(x: Double, dp: Double): String {
        if (dp.isNaN() || dp != floor(dp) || dp < 0 || dp > 1e9) {
            throw AutomationFunctionException("[BigNumber Error] Argument out of range or not an integer: " + numberToJsString(dp))
        }
        if (x.isNaN()) return "NaN"
        if (x.isInfinite()) return if (x > 0) "Infinity" else "-Infinity"
        val negative = 1 / x < 0
        val decimals = dp.toInt()
        // digits and the position of the decimal point of String(|x|)
        val string = numberToJsString(abs(x))
        val ePos = string.indexOf('e')
        val mantissaPart = if (ePos >= 0) string.substring(0, ePos) else string
        val exponent = if (ePos >= 0) string.substring(ePos + 1).toInt() else 0
        val dot = mantissaPart.indexOf('.')
        val digits = mantissaPart.replace(".", "")
        val pointPosition = (if (dot >= 0) dot else mantissaPart.length) + exponent
        // integer digits and fraction digits
        val intDigits = if (pointPosition <= 0) "0" else digits.padEnd(pointPosition, '0').substring(0, pointPosition)
        val fraction = if (pointPosition >= digits.length) "" else if (pointPosition < 0) "0".repeat(-pointPosition) + digits else digits.substring(pointPosition)
        var kept = (intDigits + fraction.padEnd(decimals, '0').substring(0, decimals))
        val next = fraction.getOrNull(decimals)
        if (next != null && next >= '5') {
            kept = incrementDigits(kept)
        }
        val integerLength = kept.length - decimals
        var result = kept.substring(0, integerLength).trimStart('0').ifEmpty { "0" }
        if (decimals > 0) result += "." + kept.substring(integerLength)
        return if (negative && x != 0.0) "-$result" else result
    }

    private fun incrementDigits(digits: String): String {
        val chars = digits.toCharArray()
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

    // ---- unformatting.js

    private val allSuffixes: List<Pair<String, Double>> = listOf(
        "ZiB" to 1024.0.pow(7), "ZB" to 1000.0.pow(7), "YiB" to 1024.0.pow(8), "YB" to 1000.0.pow(8),
        "TiB" to 1024.0.pow(4), "TB" to 1000.0.pow(4), "PiB" to 1024.0.pow(5), "PB" to 1000.0.pow(5),
        "MiB" to 1024.0.pow(2), "MB" to 1000.0.pow(2), "KiB" to 1024.0.pow(1), "KB" to 1000.0.pow(1),
        "GiB" to 1024.0.pow(3), "GB" to 1000.0.pow(3), "EiB" to 1024.0.pow(6), "EB" to 1000.0.pow(6), "B" to 1.0,
    )
    private val suffixRegexes = allSuffixes.map { (key, factor) -> Regex("([0-9 ])($key)\\z") to factor }
    private val negativeRegex = Regex("(^[^(]*)\\((.*)\\)([^)]*\\z)")

    /** numbro.unformat(string): a number, or null for undefined */
    fun unformat(input: String, language: NumbroLanguage): Any? {
        val value = if (matchesTime(input, language)) unformatTime(input) else unformatValue(input, language)
        return value ?: JsUndefined
    }

    private fun matchesTime(input: String, language: NumbroLanguage): Boolean {
        // inputString.indexOf(":") && thousands !== ":" (indexOf is -1, truthy, when there is no colon)
        if (input.indexOf(":") == 0 || language.thousands == ":") return false
        val segments = input.split(":")
        if (segments.size != 3) return false
        return segments.none { stringToJsNumber(it).isNaN() }
    }

    private fun unformatTime(input: String): Double {
        val segments = input.split(":")
        return stringToJsNumber(segments[2]) + 60 * stringToJsNumber(segments[1]) + 3600 * stringToJsNumber(segments[0])
    }

    private fun unformatValue(input: String, language: NumbroLanguage): Double? {
        if (input == "") return null
        return computeUnformattedValue(removeFormattingSymbols(input, language), language)
    }

    private fun removeFormattingSymbols(input: String, language: NumbroLanguage): String {
        var stripped = input.replaceFirst(language.currencySymbol, "")
        stripped = stripped.replace(Regex("([0-9])" + Regex.escape(language.thousands) + "([0-9])"), "$1$2")
        stripped = stripped.replaceFirst(language.decimal, ".")
        return stripped
    }

    /** null is JS undefined; arithmetic with undefined gives NaN */
    private fun computeUnformattedValue(input: String, language: NumbroLanguage): Double? {
        val number = stringToJsNumber(input)
        if (!number.isNaN()) return number

        val newInput = negativeRegex.find(input)?.let { m -> input.replaceRange(m.range, m.groupValues[1] + m.groupValues[2] + m.groupValues[3]) } ?: input
        if (newInput != input) {
            return -1 * (computeUnformattedValue(newInput, language) ?: Double.NaN)
        }

        for ((regex, factor) in suffixRegexes) {
            val stripped = regex.find(input)?.let { m -> input.replaceRange(m.range, m.groupValues[1]) } ?: input
            if (stripped != input) {
                return (computeUnformattedValue(stripped, language) ?: Double.NaN) * factor
            }
        }

        val percentStripped = input.replaceFirst("%", "")
        if (percentStripped != input) {
            return (computeUnformattedValue(percentStripped, language) ?: Double.NaN) / 100
        }

        val possibleOrdinalValue = Numbro.parseFloatJs(input)
        if (possibleOrdinalValue.isNaN()) return null

        val ordinalString = language.ordinal.of(possibleOrdinalValue)
        if (!ordinalString.isNullOrEmpty() && ordinalString != ".") {
            val stripped = input.replace(Regex(Regex.escape(ordinalString) + "\\z"), "")
            if (stripped != input) {
                return computeUnformattedValue(stripped, language)
            }
        }

        val inversed = LinkedHashMap<String, String>()
        for ((key, value) in language.abbreviations) inversed[value] = key
        for (value in inversed.keys.sortedDescending()) {
            val key = inversed.getValue(value)
            val stripped = input.replaceFirst(value, "")
            if (stripped != input) {
                val factor = when (key) {
                    "thousand" -> 1e3
                    "million" -> 1e6
                    "billion" -> 1e9
                    "trillion" -> 1e12
                    else -> Double.NaN
                }
                return (computeUnformattedValue(stripped, language) ?: Double.NaN) * factor
            }
        }
        return null
    }
}
