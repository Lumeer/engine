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

/** Ordinal suffix functions of the numbro languages (used when unformatting strings). */
internal sealed interface Ordinal {
    /** the suffix, null for JS undefined */
    fun of(number: Double): String?

    class Constant(private val value: String) : Ordinal {
        override fun of(number: Double): String = value
    }

    object English : Ordinal {
        override fun of(number: Double): String {
            val b = number % 10
            return if (jsToInt32(number % 100 / 10) == 1) "th" else when (b) {
                1.0 -> "st"
                2.0 -> "nd"
                3.0 -> "rd"
                else -> "th"
            }
        }
    }

    object Spanish : Ordinal {
        override fun of(number: Double): String {
            val t = number % 10
            return when {
                t == 1.0 || t == 3.0 -> "er"
                t == 2.0 -> "do"
                t == 7.0 || t == 0.0 -> "mo"
                t == 8.0 -> "vo"
                t == 9.0 -> "no"
                else -> "to"
            }
        }
    }

    object French : Ordinal {
        override fun of(number: Double): String = if (number == 1.0) "er" else "ème"
    }

    object Dutch : Ordinal {
        override fun of(number: Double): String {
            val t = number % 100
            return if (number != 0.0 && t <= 1 || t == 8.0 || t >= 20) "ste" else "de"
        }
    }

    object Turkish : Ordinal {
        private val suffixes = mapOf(
            "1" to "'inci", "5" to "'inci", "8" to "'inci", "70" to "'inci", "80" to "'inci", "2" to "'nci", "7" to "'nci", "20" to "'nci", "50" to "'nci",
            "3" to "'üncü", "4" to "'üncü", "100" to "'üncü", "6" to "'ncı", "9" to "'uncu", "10" to "'uncu", "30" to "'uncu", "40" to "'ıncı", "60" to "'ıncı", "90" to "'ıncı",
        )

        override fun of(number: Double): String? {
            if (number == 0.0) return "'ıncı"
            val a = number % 10
            // t[a] || t[e % 100 - a] || t[e >= 100 ? 100 : null]
            return suffixes[jsKey(a)] ?: suffixes[jsKey(number % 100 - a)] ?: (if (number >= 100) suffixes["100"] else null)
        }

        private fun jsKey(value: Double): String = io.lumeer.datafilters.util.jsString(value)
    }
}

/** ~~x */
internal fun jsToInt32(value: Double): Int {
    if (!value.isFinite()) return 0
    val truncated = kotlin.math.truncate(value) % 4294967296.0
    val unsigned = if (truncated < 0) truncated + 4294967296.0 else truncated
    return (if (unsigned >= 2147483648.0) unsigned - 4294967296.0 else unsigned).toInt()
}

/** A numbro 2.5.0 language, the parts used by formatCurrency and unformat. */
internal class NumbroLanguage(
    val tag: String,
    val thousands: String,
    val decimal: String,
    /** in the order of the language definition */
    val abbreviations: List<Pair<String, String>>,
    val currencySymbol: String,
    val currencyPosition: String?,
    val ordinal: Ordinal,
)

/** numbro 2.5.0 languages in the order they get registered: en-US (numbro itself), then all languages of the bundle. */
internal val NUMBRO_LANGUAGES: List<NumbroLanguage> = listOf(
        NumbroLanguage("en-US", ",", ".", listOf("thousand" to "k", "million" to "m", "billion" to "b", "trillion" to "t"), "\$", "prefix", Ordinal.English),
        NumbroLanguage("bg", " ", ",", listOf("thousand" to "И", "million" to "А", "billion" to "M", "trillion" to "T"), "лв.", null, Ordinal.Constant(".")),
        NumbroLanguage("cs-CZ", " ", ",", listOf("thousand" to "tis.", "million" to "mil.", "billion" to "mld.", "trillion" to "bil."), "Kč", "postfix", Ordinal.Constant(".")),
        NumbroLanguage("da-DK", ".", ",", listOf("thousand" to "t", "million" to "mio", "billion" to "mia", "trillion" to "b"), "kr", "postfix", Ordinal.Constant(".")),
        NumbroLanguage("de-AT", " ", ",", listOf("thousand" to "k", "million" to "m", "billion" to "b", "trillion" to "t"), "€", null, Ordinal.Constant(".")),
        NumbroLanguage("de-CH", "’", ".", listOf("thousand" to "k", "million" to "m", "billion" to "b", "trillion" to "t"), "CHF", "postfix", Ordinal.Constant(".")),
        NumbroLanguage("de-DE", ".", ",", listOf("thousand" to "k", "million" to "Mi", "billion" to "Ma", "trillion" to "Bi"), "€", "postfix", Ordinal.Constant(".")),
        NumbroLanguage("de-LI", "'", ".", listOf("thousand" to "k", "million" to "m", "billion" to "b", "trillion" to "t"), "CHF", "postfix", Ordinal.Constant(".")),
        NumbroLanguage("el", ".", ",", listOf("thousand" to "χ", "million" to "ε", "billion" to "δ", "trillion" to "τ"), "€", null, Ordinal.Constant(".")),
        NumbroLanguage("en-AU", ",", ".", listOf("thousand" to "k", "million" to "m", "billion" to "b", "trillion" to "t"), "\$", "prefix", Ordinal.English),
        NumbroLanguage("en-GB", ",", ".", listOf("thousand" to "k", "million" to "m", "billion" to "b", "trillion" to "t"), "£", "prefix", Ordinal.English),
        NumbroLanguage("en-IE", ",", ".", listOf("thousand" to "k", "million" to "m", "billion" to "b", "trillion" to "t"), "€", "prefix", Ordinal.English),
        NumbroLanguage("en-NZ", ",", ".", listOf("thousand" to "k", "million" to "m", "billion" to "b", "trillion" to "t"), "\$", "prefix", Ordinal.English),
        NumbroLanguage("en-ZA", " ", ",", listOf("thousand" to "k", "million" to "m", "billion" to "b", "trillion" to "t"), "R", "prefix", Ordinal.English),
        NumbroLanguage("es-AR", ".", ",", listOf("thousand" to "k", "million" to "mm", "billion" to "b", "trillion" to "t"), "\$", "postfix", Ordinal.Spanish),
        NumbroLanguage("es-CL", ".", ",", listOf("thousand" to "k", "million" to "mm", "billion" to "b", "trillion" to "t"), "\$", "prefix", Ordinal.Spanish),
        NumbroLanguage("es-CO", ".", ",", listOf("thousand" to "k", "million" to "mm", "billion" to "b", "trillion" to "t"), "€", "postfix", Ordinal.Spanish),
        NumbroLanguage("es-CR", " ", ",", listOf("thousand" to "k", "million" to "mm", "billion" to "b", "trillion" to "t"), "₡", "postfix", Ordinal.Spanish),
        NumbroLanguage("es-ES", ".", ",", listOf("thousand" to "k", "million" to "mm", "billion" to "b", "trillion" to "t"), "€", "postfix", Ordinal.Spanish),
        NumbroLanguage("es-MX", ",", ".", listOf("thousand" to "k", "million" to "mm", "billion" to "b", "trillion" to "t"), "\$", "postfix", Ordinal.Spanish),
        NumbroLanguage("es-NI", ",", ".", listOf("thousand" to "k", "million" to "mm", "billion" to "b", "trillion" to "t"), "C\$", "prefix", Ordinal.Spanish),
        NumbroLanguage("es-PE", ",", ".", listOf("thousand" to "k", "million" to "mm", "billion" to "b", "trillion" to "t"), "S/.", "prefix", Ordinal.Spanish),
        NumbroLanguage("es-PR", ",", ".", listOf("thousand" to "k", "million" to "mm", "billion" to "b", "trillion" to "t"), "\$", "prefix", Ordinal.Spanish),
        NumbroLanguage("es-SV", ",", ".", listOf("thousand" to "k", "million" to "mm", "billion" to "b", "trillion" to "t"), "\$", "prefix", Ordinal.Spanish),
        NumbroLanguage("et-EE", " ", ",", listOf("thousand" to "tuh", "million" to "mln", "billion" to "mld", "trillion" to "trl"), "€", "postfix", Ordinal.Constant(".")),
        NumbroLanguage("fa-IR", "،", ".", listOf("thousand" to "هزار", "million" to "میلیون", "billion" to "میلیارد", "trillion" to "تریلیون"), "﷼", null, Ordinal.Constant("ام")),
        NumbroLanguage("fi-FI", " ", ",", listOf("thousand" to "k", "million" to "M", "billion" to "G", "trillion" to "T"), "€", "postfix", Ordinal.Constant(".")),
        NumbroLanguage("fil-PH", ",", ".", listOf("thousand" to "k", "million" to "m", "billion" to "b", "trillion" to "t"), "₱", null, Ordinal.English),
        NumbroLanguage("fr-CA", " ", ",", listOf("thousand" to "k", "million" to "M", "billion" to "G", "trillion" to "T"), "\$", "postfix", Ordinal.French),
        NumbroLanguage("fr-CH", " ", ".", listOf("thousand" to "k", "million" to "m", "billion" to "b", "trillion" to "t"), "CHF", "postfix", Ordinal.French),
        NumbroLanguage("fr-FR", " ", ",", listOf("thousand" to "k", "million" to "M", "billion" to "Mrd", "trillion" to "billion"), "€", "postfix", Ordinal.French),
        NumbroLanguage("he-IL", ",", ".", listOf("thousand" to "אלף", "million" to "מיליון", "billion" to "מיליארד", "trillion" to "טריליון"), "₪", "prefix", Ordinal.Constant("")),
        NumbroLanguage("hu-HU", " ", ",", listOf("thousand" to "E", "million" to "M", "billion" to "Mrd", "trillion" to "T"), "Ft", "postfix", Ordinal.Constant(".")),
        NumbroLanguage("id", ".", ",", listOf("thousand" to "r", "million" to "j", "billion" to "m", "trillion" to "t"), "Rp", null, Ordinal.Constant(".")),
        NumbroLanguage("it-CH", "'", ".", listOf("thousand" to "mila", "million" to "mil", "billion" to "b", "trillion" to "t"), "CHF", null, Ordinal.Constant("°")),
        NumbroLanguage("it-IT", ".", ",", listOf("thousand" to "mila", "million" to "mil", "billion" to "b", "trillion" to "t"), "€", "postfix", Ordinal.Constant("º")),
        NumbroLanguage("ja-JP", ",", ".", listOf("thousand" to "千", "million" to "百万", "billion" to "十億", "trillion" to "兆"), "¥", "prefix", Ordinal.Constant(".")),
        NumbroLanguage("ko-KR", ",", ".", listOf("thousand" to "천", "million" to "백만", "billion" to "십억", "trillion" to "일조"), "₩", null, Ordinal.Constant(".")),
        NumbroLanguage("lv-LV", " ", ",", listOf("thousand" to "tūkst.", "million" to "milj.", "billion" to "mljrd.", "trillion" to "trilj."), "€", "postfix", Ordinal.Constant(".")),
        NumbroLanguage("nb-NO", " ", ",", listOf("thousand" to "t", "million" to "M", "billion" to "md", "trillion" to "b"), "kr", "postfix", Ordinal.Constant("")),
        NumbroLanguage("nb", " ", ",", listOf("thousand" to "t", "million" to "mil", "billion" to "mia", "trillion" to "b"), "kr", null, Ordinal.Constant(".")),
        NumbroLanguage("nl-BE", " ", ",", listOf("thousand" to "k", "million" to "mln", "billion" to "mld", "trillion" to "bln"), "€", "postfix", Ordinal.Dutch),
        NumbroLanguage("nl-NL", ".", ",", listOf("thousand" to "k", "million" to "mln", "billion" to "mrd", "trillion" to "bln"), "€", "prefix", Ordinal.Dutch),
        NumbroLanguage("nn", " ", ",", listOf("thousand" to "t", "million" to "mil", "billion" to "mia", "trillion" to "b"), "kr", null, Ordinal.Constant(".")),
        NumbroLanguage("pl-PL", " ", ",", listOf("thousand" to "tys.", "million" to "mln", "billion" to "mld", "trillion" to "bln"), " zł", "postfix", Ordinal.Constant(".")),
        NumbroLanguage("pt-BR", ".", ",", listOf("thousand" to "mil", "million" to "milhões", "billion" to "b", "trillion" to "t"), "R\$", "prefix", Ordinal.Constant("º")),
        NumbroLanguage("pt-PT", " ", ",", listOf("thousand" to "k", "million" to "m", "billion" to "b", "trillion" to "t"), "€", "postfix", Ordinal.Constant("º")),
        NumbroLanguage("ro-RO", ".", ",", listOf("thousand" to "mii", "million" to "mil", "billion" to "mld", "trillion" to "bln"), " lei", "postfix", Ordinal.Constant(".")),
        NumbroLanguage("ru-RU", " ", ",", listOf("thousand" to "тыс.", "million" to "млн", "billion" to "b", "trillion" to "t"), "руб.", "postfix", Ordinal.Constant(".")),
        NumbroLanguage("ru-UA", " ", ",", listOf("thousand" to "тыс.", "million" to "млн", "billion" to "b", "trillion" to "t"), "₴", "postfix", Ordinal.Constant(".")),
        NumbroLanguage("sk-SK", " ", ",", listOf("thousand" to "tis.", "million" to "mil.", "billion" to "mld.", "trillion" to "bil."), "€", "postfix", Ordinal.Constant(".")),
        NumbroLanguage("sl", ".", ",", listOf("thousand" to "tis.", "million" to "mil.", "billion" to "b", "trillion" to "t"), "€", null, Ordinal.Constant(".")),
        NumbroLanguage("sr-Cyrl-RS", ".", ",", listOf("thousand" to "тыс.", "million" to "млн", "billion" to "b", "trillion" to "t"), "RSD", null, Ordinal.Constant(".")),
        NumbroLanguage("sv-SE", " ", ",", listOf("thousand" to "t", "million" to "M", "billion" to "md", "trillion" to "tmd"), "kr", "postfix", Ordinal.Constant("")),
        NumbroLanguage("th-TH", ",", ".", listOf("thousand" to "พัน", "million" to "ล้าน", "billion" to "พันล้าน", "trillion" to "ล้านล้าน"), "฿", "postfix", Ordinal.Constant(".")),
        NumbroLanguage("tr-TR", ".", ",", listOf("thousand" to "bin", "million" to "milyon", "billion" to "milyar", "trillion" to "trilyon"), "₺", "postfix", Ordinal.Turkish),
        NumbroLanguage("uk-UA", " ", ",", listOf("thousand" to "тис.", "million" to "млн", "billion" to "млрд", "trillion" to "блн"), "₴", "postfix", Ordinal.Constant("")),
        NumbroLanguage("zh-CN", ",", ".", listOf("thousand" to "千", "million" to "百万", "billion" to "十亿", "trillion" to "兆"), "¥", "prefix", Ordinal.Constant(".")),
        NumbroLanguage("zh-MO", ",", ".", listOf("thousand" to "千", "million" to "百萬", "billion" to "十億", "trillion" to "兆"), "MOP", null, Ordinal.Constant(".")),
        NumbroLanguage("zh-SG", ",", ".", listOf("thousand" to "千", "million" to "百万", "billion" to "十亿", "trillion" to "兆"), "\$", null, Ordinal.Constant(".")),
        NumbroLanguage("zh-TW", ",", ".", listOf("thousand" to "千", "million" to "百萬", "billion" to "十億", "trillion" to "兆"), "NT\$", null, Ordinal.Constant("第")),
)
