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
package io.lumeer.datafilters.date

import kotlin.math.abs
import kotlin.math.floor

/** moment.js formatting for the "en" locale. */
internal object DateFormatter {

    val FORMATTING_TOKENS = Regex("(\\[[^\\[]*\\])|(\\\\)?([Hh]mm(ss)?|Mo|MM?M?M?|Do|DDDo|DD?D?D?|ddd?d?|do?|w[o|w]?|W[o|W]?|Qo?|N{1,5}|YYYYYY|YYYYY|YYYY|YY|y{2,4}|yo?|gg(ggg?)?|GG(GGG?)?|e|E|a|A|hh?|HH?|kk?|mm?|ss?|S{1,9}|x|X|zz?|ZZ?|.)")
    /** moment 2.31: an escaped e and the eHHmm token */
    val FORMATTING_TOKENS_LATEST = Regex("(\\[[^\\[]*\\])|(\\\\e)|(\\\\)?(eHHmm|[Hh]mm(ss)?|Mo|MM?M?M?|Do|DDDo|DD?D?D?|ddd?d?|do?|w[o|w]?|W[o|W]?|Qo?|N{1,5}|YYYYYY|YYYYY|YYYY|YY|y{2,4}|yo?|gg(ggg?)?|GG(GGG?)?|e|E|a|A|hh?|HH?|kk?|mm?|ss?|S{1,9}|x|X|zz?|ZZ?|.)")

    fun formattingTokens(latest: Boolean): Regex = if (latest) FORMATTING_TOKENS_LATEST else FORMATTING_TOKENS

    private val LOCAL_FORMATTING_TOKENS = Regex("(\\[[^\\[]*\\])|(\\\\)?(LTS|LT|LL?L?L?|l{1,4})")

    val MONTHS_SHORT: List<String> = DateLocale.En.monthShortNames
    val WEEKDAYS_SHORT: List<String> = DateLocale.En.weekdaysShort

    /** Tokens that have a formatting function (others are literals). */
    val FORMAT_TOKENS: Set<String> = buildSet {
        addAll(listOf("M", "MM", "Mo", "MMM", "MMMM", "Q", "Qo", "D", "DD", "Do", "DDD", "DDDD", "DDDo", "d", "do", "dd", "ddd", "dddd", "e", "E"))
        addAll(listOf("w", "ww", "wo", "W", "WW", "Wo", "Y", "YY", "YYYY", "YYYYY", "YYYYYY", "gg", "gggg", "ggggg", "GG", "GGGG", "GGGGG"))
        addAll(listOf("H", "HH", "h", "hh", "k", "kk", "hmm", "hmmss", "Hmm", "Hmmss", "a", "A", "m", "mm", "s", "ss"))
        var s = "S"
        while (s.length <= 9) {
            add(s)
            s += "S"
        }
        addAll(listOf("Z", "ZZ", "z", "zz", "X", "x", "N", "NN", "NNN", "NNNN", "NNNNN", "y", "yy", "yyy", "yyyy", "yo", "eHHmm"))
    }

    fun expandFormat(format: String, locale: DateLocale = DateLocale.En): String {
        var result = format
        var i = 5
        while (i >= 0 && LOCAL_FORMATTING_TOKENS.containsMatchIn(result)) {
            result = result.replace(LOCAL_FORMATTING_TOKENS) { locale.longDateFormat(it.value) ?: it.value }
            i -= 1
        }
        return result
    }

    /** [latest]: moment 2.31 (automations) instead of the moment version of the original filters bundle */
    fun format(mom: DateInstant, format: String, locale: DateLocale = DateLocale.En, latest: Boolean = false): String {
        val expanded = expandFormat(format, locale)
        val output = StringBuilder()
        for (match in formattingTokens(latest).findAll(expanded)) {
            val token = match.value
            if (token in FORMAT_TOKENS) {
                output.append(formatToken(mom, token, locale, expanded, latest))
            } else {
                output.append(removeFormattingTokens(token))
            }
        }
        return output.toString()
    }

    private val BRACKET_LITERAL = Regex("\\[[\\s\\S]")
    private val OUTER_BRACKETS = Regex("^\\[|\\]$")

    private fun removeFormattingTokens(input: String): String =
        if (BRACKET_LITERAL.containsMatchIn(input)) input.replace(OUTER_BRACKETS, "") else input.replace("\\", "")

    fun zeroFill(number: Long, targetLength: Int, forceSign: Boolean = false): String {
        val absNumber = abs(number).toString()
        val zerosToFill = targetLength - absNumber.length
        val sign = if (number >= 0) (if (forceSign) "+" else "") else "-"
        return sign + "0".repeat(maxOf(0, zerosToFill)) + absNumber
    }

    private fun eraYear(mom: DateInstant): Long = if (mom.year() >= 1) mom.year() else 1 - mom.year()

    private fun hFormat(mom: DateInstant): Long = (mom.hours() % 12).let { if (it == 0) 12 else it }.toLong()

    private fun offset(mom: DateInstant, separator: String, latest: Boolean): String {
        var offset = if (latest) mom.utcOffsetRounded else mom.utcOffset
        var sign = "+"
        if (offset < 0) {
            offset = -offset
            sign = "-"
        }
        return sign + zeroFill((offset / 60).toLong(), 2) + separator + zeroFill((offset % 60).toLong(), 2)
    }

    private fun formatToken(mom: DateInstant, token: String, locale: DateLocale, format: String, latest: Boolean): String {
        fun ordinal(number: Long, token: String) = locale.ordinal(number, token)
        fun week() = DateInstant.weekOfYear(mom, locale.dow, locale.doy)
        return formatToken(mom, token, locale, format, latest, ::ordinal, ::week)
    }

    private fun formatToken(mom: DateInstant, token: String, locale: DateLocale, format: String, latest: Boolean, ordinal: (Long, String) -> String, week: () -> Pair<Int, Long>): String = when (token) {
        "M" -> (mom.month() + 1).toString()
        "MM" -> zeroFill(mom.month() + 1L, 2)
        "Mo" -> ordinal(mom.month() + 1L, "M")
        "MMM" -> locale.monthsShort(mom.month(), format)
        "MMMM" -> locale.months(mom.month(), format)
        "Q" -> mom.quarter().toString()
        "Qo" -> ordinal(mom.quarter().toLong(), "Q")
        "D" -> mom.date().toString()
        "DD" -> zeroFill(mom.date().toLong(), 2)
        "Do" -> ordinal(mom.date().toLong(), "D")
        "DDD" -> mom.dayOfYear().toString()
        "DDDD" -> zeroFill(mom.dayOfYear().toLong(), 3)
        "DDDo" -> ordinal(mom.dayOfYear().toLong(), "DDD")
        "d" -> mom.day().toString()
        "do" -> ordinal(mom.day().toLong(), "d")
        "dd" -> locale.weekdaysMin[mom.day()]
        "ddd" -> locale.weekdaysShort[mom.day()]
        "dddd" -> locale.weekdays(mom.day(), format)
        "e" -> ((mom.day() + 7 - locale.dow) % 7).toString()
        "E" -> mom.isoWeekday().toString()
        "eHHmm" -> ((mom.day() + 7 - locale.dow) % 7).toString() + zeroFill(mom.hours().toLong(), 2) + zeroFill(mom.minutes().toLong(), 2)
        "w" -> week().first.toString()
        "ww" -> zeroFill(week().first.toLong(), 2)
        "wo" -> ordinal(week().first.toLong(), "w")
        "W" -> mom.isoWeek().toString()
        "WW" -> zeroFill(mom.isoWeek().toLong(), 2)
        "Wo" -> ordinal(mom.isoWeek().toLong(), "W")
        "Y" -> mom.year().let { if (it <= 9999) zeroFill(it, 4) else "+$it" }
        "YY" -> zeroFill(mom.year() % 100, 2)
        "YYYY" -> zeroFill(mom.year(), 4)
        "YYYYY" -> zeroFill(mom.year(), 5)
        "YYYYYY" -> zeroFill(mom.year(), 6, true)
        "gg" -> zeroFill(week().second % 100, 2)
        "gggg" -> zeroFill(week().second, 4)
        "ggggg" -> zeroFill(week().second, 5)
        "GG" -> zeroFill(mom.isoWeekYear() % 100, 2)
        "GGGG" -> zeroFill(mom.isoWeekYear(), 4)
        "GGGGG" -> zeroFill(mom.isoWeekYear(), 5)
        "H" -> mom.hours().toString()
        "HH" -> zeroFill(mom.hours().toLong(), 2)
        "h" -> hFormat(mom).toString()
        "hh" -> zeroFill(hFormat(mom), 2)
        "k" -> (if (mom.hours() == 0) 24 else mom.hours()).toString()
        "kk" -> zeroFill((if (mom.hours() == 0) 24 else mom.hours()).toLong(), 2)
        "hmm" -> hFormat(mom).toString() + zeroFill(mom.minutes().toLong(), 2)
        "hmmss" -> hFormat(mom).toString() + zeroFill(mom.minutes().toLong(), 2) + zeroFill(mom.seconds().toLong(), 2)
        "Hmm" -> mom.hours().toString() + zeroFill(mom.minutes().toLong(), 2)
        "Hmmss" -> mom.hours().toString() + zeroFill(mom.minutes().toLong(), 2) + zeroFill(mom.seconds().toLong(), 2)
        "a" -> locale.meridiem(mom.hours(), true)
        "A" -> locale.meridiem(mom.hours(), false)
        "m" -> mom.minutes().toString()
        "mm" -> zeroFill(mom.minutes().toLong(), 2)
        "s" -> mom.seconds().toString()
        "ss" -> zeroFill(mom.seconds().toLong(), 2)
        "S" -> (mom.milliseconds() / 100).toString()
        "SS" -> zeroFill((mom.milliseconds() / 10).toLong(), 2)
        "SSS" -> zeroFill(mom.milliseconds().toLong(), 3)
        "Z" -> offset(mom, ":", latest)
        "ZZ" -> offset(mom, "", latest)
        "z", "zz" -> mom.zoneAbbr()
        "X" -> floor(mom.valueOf / 1000.0).toLong().toString()
        "x" -> mom.valueOf.toString()
        "N", "NN", "NNN", "NNNNN" -> if (mom.year() >= 1) "AD" else "BC"
        "NNNN" -> if (mom.year() >= 1) "Anno Domini" else "Before Christ"
        "y" -> zeroFill(eraYear(mom), 1)
        "yy" -> zeroFill(eraYear(mom), 2)
        "yyy" -> zeroFill(eraYear(mom), 3)
        "yyyy" -> zeroFill(eraYear(mom), 4)
        "yo" -> ordinal(eraYear(mom), "y")
        else -> {
            // SSSS .. SSSSSSSSS
            val multiplier = "1" + "0".repeat(token.length - 3)
            zeroFill(mom.milliseconds() * multiplier.toLong(), token.length)
        }
    }
}
