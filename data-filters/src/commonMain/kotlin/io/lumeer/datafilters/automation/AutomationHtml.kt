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

/** HTML functions of the automation blocks, replacing he (2.0.0): he.escape and he.unescape (an alias of he.decode). */
object AutomationHtml {

    private fun parseMap(data: String, keyRadix: Int? = null): Map<String, String> = data.split(",").associate { entry ->
        val (key, value) = entry.split("=")
        val name = if (keyRadix != null) key.toInt(keyRadix).toString() else key
        name to value.split("+").joinToString("") { fromCodePoint(it.toInt(16)) }
    }

    private val decodeMap: Map<String, String> by lazy { parseMap(HtmlEntityData.DECODE_MAP) }
    private val decodeMapLegacy: Map<String, String> by lazy { parseMap(HtmlEntityData.DECODE_MAP_LEGACY) }
    private val decodeMapNumeric: Map<String, String> by lazy { parseMap(HtmlEntityData.DECODE_MAP_NUMERIC, 16) }
    private val decodeRegex: Regex by lazy { Regex(HtmlEntityData.DECODE_REGEX) }

    private val escapeMap = mapOf('"' to "&quot;", '&' to "&amp;", '\'' to "&#x27;", '<' to "&lt;", '>' to "&gt;", '`' to "&#x60;")

    private fun requireString(value: Any?): String =
        value as? String ?: throw AutomationFunctionException("he: the value is not a string ($value)")

    /** Block "escape HTML": he.escape(value) */
    fun escape(value: Any?): String {
        val string = requireString(value)
        return buildString(string.length) {
            for (c in string) append(escapeMap[c] ?: c.toString())
        }
    }

    /** Block "unescape HTML": he.unescape(value) */
    fun unescape(value: Any?): String {
        val html = requireString(value)
        return decodeRegex.replace(html) { match ->
            val g = match.groups
            val named = g[1]?.value
            val legacy = g[2]?.value
            val decimal = g[4]?.value
            val hex = g[6]?.value
            when {
                !named.isNullOrEmpty() -> decodeMap.getValue(named)
                !legacy.isNullOrEmpty() -> decodeMapLegacy.getValue(legacy) + (g[3]?.value ?: "")
                !decimal.isNullOrEmpty() -> codePointToSymbol(parseDigits(decimal, 10))
                !hex.isNullOrEmpty() -> codePointToSymbol(parseDigits(hex, 16))
                else -> match.value
            }
        }
    }

    /** parseInt of a run of digits */
    private fun parseDigits(digits: String, radix: Int): Double = digits.fold(0.0) { result, c -> result * radix + c.digitToInt(radix) }

    private fun codePointToSymbol(codePoint: Double): String {
        if ((codePoint >= 0xD800 && codePoint <= 0xDFFF) || codePoint > 0x10FFFF) {
            return fromCodePoint(0xFFFD)
        }
        val key = codePoint.toInt().toString()
        return decodeMapNumeric[key] ?: fromCodePoint(codePoint.toInt())
    }

    private fun fromCodePoint(codePoint: Int): String = if (codePoint < 0x10000) {
        codePoint.toChar().toString()
    } else {
        val offset = codePoint - 0x10000
        charArrayOf((0xD800 + (offset shr 10)).toChar(), (0xDC00 + (offset and 0x3FF)).toChar()).concatToString()
    }
}
