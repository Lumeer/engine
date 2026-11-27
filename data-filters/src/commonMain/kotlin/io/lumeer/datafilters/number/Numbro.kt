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

import io.lumeer.datafilters.model.CurrencyData
import io.lumeer.datafilters.util.jsTrim
import io.lumeer.datafilters.util.numberToJsString
import io.lumeer.datafilters.util.stringToJsNumber
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.truncate

internal const val USA = "en-US"

internal class Currency(
    val symbol: String,
    val code: String,
    val prefix: Boolean,
    val spaceSeparated: Boolean,
    val decimalSeparator: String,
    val thousandSeparator: String,
)

/** currency.utils.ts */
internal val CURRENCIES: Map<String, Currency> = mapOf(
        "da-DK" to Currency("kr", "DKK", false, true, ",", "."),
        "de-CH" to Currency("CHF", "CHF", false, true, ",", "’"),
        "en-NZ" to Currency("$", "NZD", true, true, ".", ","),
        "fr-CA" to Currency("$", "CAD", false, true, ".", "\u00A0"),
        "en-CA" to Currency("$", "CAD", true, false, ".", ","),
        "sv-SE" to Currency("kr", "SEK", false, true, ",", "\u00A0"),
        "nb-NO" to Currency("kr", "NOK", false, true, ",", "\u00A0"),
        "fi-FI" to Currency("€", "EUR", false, true, ",", "\u00A0"),
        "he-IL" to Currency("₪", "ILS", true, true, ".", ","),
        "es-ES" to Currency("€", "EUR", false, true, ",", "."),
        "fr-FR" to Currency("€", "EUR", false, true, ",", "\u00A0"),
        "it-IT" to Currency("€", "EUR", false, true, ",", "."),
        "en-GB" to Currency("£", "GBP", true, true, ".", ","),
        "pt-PT" to Currency("€", "EUR", false, true, ",", "\u00A0"),
        "pl-PL" to Currency("zł", "PLN", false, true, ",", "\u00A0"),
        "cs-CZ" to Currency("Kč", "CZK", false, true, ",", "\u00A0"),
        "sk-SK" to Currency("€", "EUR", false, true, ",", "\u00A0"),
        "hu-HU" to Currency("Ft", "HUF", false, true, ",", "\u00A0"),
        "de-AT" to Currency("€", "EUR", true, true, ",", "\u00A0"),
        "de-DE" to Currency("€", "EUR", false, true, ",", "."),
        "en-US" to Currency("$", "USD", true, false, ".", ","),
        "pt-BR" to Currency("R$", "BRL", true, false, ",", "."),
        "zh-TW" to Currency("NT$", "TWD", true, false, ".", ","),
        "nl-NL" to Currency("€", "EUR", true, true, ",", "."),
        "zh-CN" to Currency("¥", "CNY", true, false, ".", ","),
        "ru-RU" to Currency("₽", "RUB", false, false, ",", "\u00A0"),
        "ja-JP" to Currency("¥", "JPY", true, true, ".", ","),
        "en-AU" to Currency("$", "AUD", true, true, ".", ","),
        "en-IE" to Currency("€", "EUR", true, false, ".", ","),
        "en-MT" to Currency("€", "EUR", true, false, ".", ","),
        "tr-TR" to Currency("\u20BA", "TRY", false, true, ",", "."),
        "uk-UA" to Currency("\u20B4", "UAH", false, false, ",", "\u00A0"),
        "en-IN" to Currency("₹", "INR", true, false, ".", ","),
)

/** Numbro language as created by getNumbroLanguage in currency.utils.ts (unknown tags fall back to en-US) */
internal class NumbroLanguage(tag: String, data: CurrencyData?) {
    private val currency = CURRENCIES[tag] ?: CURRENCIES.getValue(USA)
    val thousands: String = currency.thousandSeparator
    val decimal: String = currency.decimalSeparator
    val abbreviations: Map<String, String> = mapOf(
        "thousand" to data?.abbreviations?.getOrNull(0).orEmpty(),
        "million" to data?.abbreviations?.getOrNull(1).orEmpty(),
        "billion" to data?.abbreviations?.getOrNull(2).orEmpty(),
        "trillion" to data?.abbreviations?.getOrNull(3).orEmpty(),
    )

    /** abbreviation -> power name, in the order numbro tries them (reverse sorted) */
    val abbreviationsToUnformat: List<Pair<String, String>> by lazy {
        val inversed = linkedMapOf<String, String>()
        for ((key, value) in abbreviations) inversed[value] = key
        inversed.keys.sortedDescending().map { it to inversed.getValue(it) }
    }

    val thousandsRegex: Regex by lazy { Regex("([0-9])" + Numbro.escapeRegExp(thousands) + "([0-9])") }
    private val ordinals: List<String> = data?.ordinals.orEmpty()
    val currencySymbol: String = currency.symbol
    val currencyPrefix: Boolean = currency.prefix
    val spaceSeparatedCurrency: Boolean = currency.spaceSeparated

    fun ordinal(number: Double): String {
        val b = number % 10
        return if (truncate((number % 100) / 10) == 1.0) ordinals.getOrNull(3).orEmpty() else when (b) {
            1.0 -> ordinals.getOrNull(0).orEmpty()
            2.0 -> ordinals.getOrNull(1).orEmpty()
            3.0 -> ordinals.getOrNull(2).orEmpty()
            else -> ordinals.getOrNull(3).orEmpty()
        }
    }

}

/** The numbro global language, the original library switches it as values are created. */
internal class NumbroState(private val currencyData: CurrencyData?) {
    private val languages = HashMap<String, NumbroLanguage>()

    var language: NumbroLanguage = NumbroLanguage(USA, null)
        private set

    fun setLanguage(tag: String) {
        language = languages.getOrPut(tag) { NumbroLanguage(tag, currencyData) }
    }
}

/** Subset of numbro format options used by Lumeer. Null means "not provided". */
internal class NumbroFormat(
    val mantissa: Int? = null,
    val trimMantissa: Boolean? = null,
    val optionalMantissa: Boolean? = null,
    val thousandSeparated: Boolean? = null,
    val spaceSeparated: Boolean? = null,
    val spaceSeparatedCurrency: Boolean? = null,
    val average: Boolean? = null,
    val lowPrecision: Boolean? = null,
    val forceSign: Boolean? = null,
    val negative: String? = null,
)

internal object Numbro {

    private val POWERS = mapOf("trillion" to 1e12, "billion" to 1e9, "million" to 1e6, "thousand" to 1e3)

    /** JS Math.round */
    fun jsRound(value: Double): Double {
        if (value.isNaN() || value.isInfinite()) return value
        val r = floor(value)
        return if (value - r >= 0.5) r + 1 else r
    }

    // ---- unformatting

    /** byte suffixes in numbro's order, with the regex matching them at the end of a number */
    private val ALL_SUFFIXES: List<Pair<Regex, Double>> = listOf(
        "ZiB" to 1024.0.pow(7), "ZB" to 1000.0.pow(7), "YiB" to 1024.0.pow(8), "YB" to 1000.0.pow(8),
        "TiB" to 1024.0.pow(4), "TB" to 1000.0.pow(4), "PiB" to 1024.0.pow(5), "PB" to 1000.0.pow(5),
        "MiB" to 1024.0.pow(2), "MB" to 1000.0.pow(2), "KiB" to 1024.0, "KB" to 1000.0,
        "GiB" to 1024.0.pow(3), "GB" to 1000.0.pow(3), "EiB" to 1024.0.pow(6), "EB" to 1000.0.pow(6), "B" to 1.0,
    ).map { (key, factor) -> Regex("([0-9 ])($key)$") to factor }

    private val REGEX_SPECIAL = Regex("[-/\\\\^$*+?.()|\\[\\]{}]")
    private val NEGATIVE_PARENTHESES = Regex("(^[^(]*)\\((.*)\\)([^)]*$)")
    private val PARSE_FLOAT = Regex("^[+-]?(\\d+\\.?\\d*|\\.\\d+)([eE][+-]?\\d+)?")
    private val ONLY_ZEROS = Regex("^0+$")
    private val TRAILING_ZEROS = Regex("0+$")

    fun escapeRegExp(s: String): String = s.replace(REGEX_SPECIAL) { "\\" + it.value }

    /** numbro.unformat: returns null for undefined */
    fun unformat(input: Any?, language: NumbroLanguage): Double? = when (input) {
        is String -> if (matchesTime(input, language)) unformatTime(input) else unformatValue(input, language)
        is Number -> input.toDouble()
        else -> null
    }

    private fun matchesTime(input: String, language: NumbroLanguage): Boolean {
        val separators = input.indexOf(':') != 0 && language.thousands != ":"
        if (!separators) return false
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
        val stripped = input
            .replaceFirst(language.currencySymbol, "")
            .replace(language.thousandsRegex, "$1$2")
            .replaceFirst(language.decimal, ".")
        return computeUnformattedValue(stripped, language)
    }

    private fun computeUnformattedValue(input: String, language: NumbroLanguage): Double? {
        val plus = stringToJsNumber(input)
        if (!plus.isNaN()) return plus

        // Negative
        val newInput = NEGATIVE_PARENTHESES.find(input)?.let { m -> m.groupValues[1] + m.groupValues[2] + m.groupValues[3] } ?: input
        if (newInput != input) {
            return computeUnformattedValue(newInput, language)?.let { -it }
        }

        // Byte
        for ((regex, factor) in ALL_SUFFIXES) {
            val match = regex.find(input)
            if (match != null) {
                val stripped = input.substring(0, match.range.first) + match.groupValues[1]
                return computeUnformattedValue(stripped, language)?.let { it * factor }
            }
        }

        // Percent
        if (input.contains("%")) {
            return computeUnformattedValue(input.replaceFirst("%", ""), language)?.let { it / 100 }
        }

        // Ordinal
        val possibleOrdinalValue = parseFloatJs(input)
        if (possibleOrdinalValue.isNaN()) return null
        val ordinalString = language.ordinal(possibleOrdinalValue)
        if (ordinalString.isNotEmpty() && ordinalString != ".") {
            val stripped = input.replace(Regex(escapeRegExp(ordinalString) + "$"), "")
            if (stripped != input) return computeUnformattedValue(stripped, language)
        }

        // Average
        for ((value, key) in language.abbreviationsToUnformat) {
            val stripped = input.replaceFirst(value, "")
            if (stripped != input) {
                val factor = POWERS[key] ?: Double.NaN
                return computeUnformattedValue(stripped, language)?.let { it * factor }
            }
        }
        return null
    }

    /** JS parseFloat */
    fun parseFloatJs(input: String): Double {
        val trimmed = jsTrim(input)
        if (trimmed.startsWith("Infinity") || trimmed.startsWith("+Infinity")) return Double.POSITIVE_INFINITY
        if (trimmed.startsWith("-Infinity")) return Double.NEGATIVE_INFINITY
        val match = PARSE_FLOAT.find(trimmed) ?: return Double.NaN
        return match.value.toDouble()
    }

    // ---- formatting

    private class Average(val value: Double, val abbreviation: String)

    private fun computeAverage(value: Double, lowPrecision: Boolean, abbreviations: Map<String, String>, spaceSeparated: Boolean): Average {
        var abbreviation = ""
        var result = value
        val abs = abs(value)
        if (abs >= 1e12 || (lowPrecision && jsRound(abs / 1e12) == 1.0)) {
            abbreviation = abbreviations.getValue("trillion")
            result = value / 1e12
        } else if (abs < 1e12 && abs >= 1e9 || (lowPrecision && jsRound(abs / 1e9) == 1.0)) {
            abbreviation = abbreviations.getValue("billion")
            result = value / 1e9
        } else if (abs < 1e9 && abs >= 1e6 || (lowPrecision && jsRound(abs / 1e6) == 1.0)) {
            abbreviation = abbreviations.getValue("million")
            result = value / 1e6
        } else if (abs < 1e6 && abs >= 1e3 || (lowPrecision && jsRound(abs / 1e3) == 1.0)) {
            abbreviation = abbreviations.getValue("thousand")
            result = value / 1e3
        }
        if (abbreviation.isNotEmpty()) {
            abbreviation = (if (spaceSeparated) " " else "") + abbreviation
        }
        return Average(result, abbreviation)
    }

    private fun toFixedLarge(value: Double, precision: Int): String {
        val str = numberToJsString(value)
        val (base, expStr) = str.split("e")
        val exp = stringToJsNumber(expStr).toInt()
        val parts = base.split(".")
        val characteristic = parts[0]
        val mantissa = parts.getOrElse(1) { "" }
        var result: String
        if (exp > 0) {
            result = characteristic + mantissa + "0".repeat(max(0, exp - mantissa.length))
        } else {
            val prefix = if (stringToJsNumber(characteristic) < 0) "-0." else "0."
            var suffix = ("0".repeat(max(0, -exp - 1)) + abs(stringToJsNumber(characteristic)).let { numberToJsString(it) } + mantissa).take(precision)
            if (suffix.length < precision) suffix += "0".repeat(precision - suffix.length)
            result = prefix + suffix
        }
        if (exp > 0 && precision > 0) {
            result += "." + "0".repeat(precision)
        }
        return result
    }

    private fun toFixed(value: Double, precision: Int): String {
        if (numberToJsString(value).contains("e")) return toFixedLarge(value, precision)
        val shifted = stringToJsNumber(numberToJsString(value) + "e+" + precision)
        val n = jsRound(shifted) / 10.0.pow(precision)
        val decimal = Decimal.parse(numberToJsString(n)) ?: return numberToJsString(n)
        return decimal.roundHalfUp(precision).toFixedString(precision)
    }

    private fun setMantissaPrecision(output: String, value: Double, optionalMantissa: Boolean, precision: Int, trim: Boolean): String {
        if (precision == -1) return output
        val result = toFixed(value, precision)
        val parts = result.split(".")
        val characteristic = parts[0]
        val mantissa = parts.getOrElse(1) { "" }
        if (ONLY_ZEROS.matches(mantissa) && (optionalMantissa || trim)) return characteristic
        val trailing = TRAILING_ZEROS.find(mantissa)
        if (trim && trailing != null) return characteristic + "." + mantissa.substring(0, trailing.range.first)
        return result
    }

    private fun replaceDelimiters(output: String, value: Double, thousandSeparated: Boolean, language: NumbroLanguage, decimalSeparator: String?): String {
        val separator = decimalSeparator ?: language.decimal
        val parts = output.split(".")
        var characteristic = parts[0]
        val mantissa = parts.getOrNull(1)
        val hasNegativeSign = value < 0 && characteristic.startsWith("-")
        if (thousandSeparated) {
            if (hasNegativeSign) characteristic = characteristic.substring(1)
            val indexes = mutableListOf<Int>()
            var counter = 0
            for (i in characteristic.length downTo 1) {
                if (counter == 3) {
                    indexes.add(0, i)
                    counter = 0
                }
                counter++
            }
            indexes.forEachIndexed { index, position ->
                characteristic = characteristic.substring(0, position + index) + language.thousands + characteristic.substring(position + index)
            }
            if (hasNegativeSign) characteristic = "-$characteristic"
        }
        return if (mantissa.isNullOrEmpty()) characteristic else characteristic + separator + mantissa
    }

    private fun insertSign(output: String, value: Double, negative: String): String {
        if (value == 0.0) return output
        if (stringToJsNumber(output) == 0.0) return output.replaceFirst("-", "")
        if (value > 0) return "+$output"
        if (negative == "sign") return output
        return "(" + output.replaceFirst("-", "") + ")"
    }

    /** numbro formatNumber */
    fun formatNumber(value: Double, format: NumbroFormat, language: NumbroLanguage, decimalSeparator: String? = null): String {
        if (value.isNaN() || value.isInfinite()) return numberToJsString(value)
        val average = format.average ?: false
        val mantissaPrecision = if (average && format.mantissa == null) 0 else format.mantissa ?: -1
        val optionalMantissa = format.optionalMantissa ?: (mantissaPrecision == -1)
        val trimMantissa = format.trimMantissa ?: false
        val thousandSeparated = format.thousandSeparated ?: false
        val spaceSeparated = format.spaceSeparated ?: false
        val negative = format.negative ?: "sign"
        val forceSign = format.forceSign ?: false

        var current = value
        var abbreviation = ""
        if (average) {
            val data = computeAverage(value, format.lowPrecision ?: true, language.abbreviations, spaceSeparated)
            current = data.value
            abbreviation += data.abbreviation
        }

        var output = setMantissaPrecision(numberToJsString(current), current, optionalMantissa, mantissaPrecision, trimMantissa)
        output = replaceDelimiters(output, current, thousandSeparated, language, decimalSeparator)
        if (average) output += abbreviation
        if (forceSign || current < 0) output = insertSign(output, current, negative)
        return output
    }

    /** numbro formatCurrency */
    fun formatCurrency(value: Double, format: NumbroFormat, language: NumbroLanguage): String {
        val average = format.average ?: false
        val spaceSeparatedCurrency = format.spaceSeparatedCurrency ?: (format.spaceSeparated ?: false)
        var space = if (spaceSeparatedCurrency) " " else ""
        val cloned = NumbroFormat(
            mantissa = format.mantissa,
            trimMantissa = format.trimMantissa,
            optionalMantissa = format.optionalMantissa,
            thousandSeparated = format.thousandSeparated,
            spaceSeparated = format.spaceSeparated,
            spaceSeparatedCurrency = format.spaceSeparatedCurrency,
            average = format.average,
            lowPrecision = format.lowPrecision ?: false,
            forceSign = format.forceSign,
            negative = format.negative,
        )
        var output = formatNumber(value, cloned, language)
        val symbol = language.currencySymbol
        if (language.currencyPrefix) {
            output = if (value < 0 && (format.negative ?: "sign") == "sign") {
                "-" + space + symbol + output.substring(1)
            } else if (value > 0 && (format.forceSign ?: false)) {
                "+" + space + symbol + output.substring(1)
            } else {
                symbol + space + output
            }
        } else {
            space = if (average) "" else space
            output = output + space + symbol
        }
        return output
    }
}
