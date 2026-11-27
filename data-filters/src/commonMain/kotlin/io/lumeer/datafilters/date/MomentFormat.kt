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
internal object MomentFormat {

    val FORMATTING_TOKENS = Regex("(\\[[^\\[]*\\])|(\\\\)?([Hh]mm(ss)?|Mo|MM?M?M?|Do|DDDo|DD?D?D?|ddd?d?|do?|w[o|w]?|W[o|W]?|Qo?|N{1,5}|YYYYYY|YYYYY|YYYY|YY|y{2,4}|yo?|gg(ggg?)?|GG(GGG?)?|e|E|a|A|hh?|HH?|kk?|mm?|ss?|S{1,9}|x|X|zz?|ZZ?|.)")
    private val LOCAL_FORMATTING_TOKENS = Regex("(\\[[^\\[]*\\])|(\\\\)?(LTS|LT|LL?L?L?|l{1,4})")

    val MONTHS = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    val MONTHS_SHORT = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val WEEKDAYS = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    val WEEKDAYS_SHORT = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val WEEKDAYS_MIN = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")

    private val LONG_DATE_FORMATS = mapOf(
        "LTS" to "h:mm:ss A",
        "LT" to "h:mm A",
        "L" to "MM/DD/YYYY",
        "LL" to "MMMM D, YYYY",
        "LLL" to "MMMM D, YYYY h:mm A",
        "LLLL" to "dddd, MMMM D, YYYY h:mm A",
    )

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
        addAll(listOf("Z", "ZZ", "z", "zz", "X", "x", "N", "NN", "NNN", "NNNN", "NNNNN", "y", "yy", "yyy", "yyyy", "yo"))
    }

    fun longDateFormat(key: String): String? {
        LONG_DATE_FORMATS[key]?.let { return it }
        val upper = LONG_DATE_FORMATS[key.uppercase()] ?: return null
        return FORMATTING_TOKENS.findAll(upper).joinToString("") { match ->
            val token = match.value
            if (token == "MMMM" || token == "MM" || token == "DD" || token == "dddd") token.substring(1) else token
        }
    }

    fun expandFormat(format: String): String {
        var result = format
        var i = 5
        while (i >= 0 && LOCAL_FORMATTING_TOKENS.containsMatchIn(result)) {
            result = result.replace(LOCAL_FORMATTING_TOKENS) { longDateFormat(it.value) ?: it.value }
            i -= 1
        }
        return result
    }

    fun format(mom: Moment, format: String): String {
        val expanded = expandFormat(format)
        val output = StringBuilder()
        for (match in FORMATTING_TOKENS.findAll(expanded)) {
            val token = match.value
            if (token in FORMAT_TOKENS) {
                output.append(formatToken(mom, token))
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

    fun ordinal(number: Long): String {
        val b = number % 10
        val suffix = if ((number % 100) / 10 == 1L) "th" else when (b) {
            1L -> "st"
            2L -> "nd"
            3L -> "rd"
            else -> "th"
        }
        return number.toString() + suffix
    }

    private fun eraYear(mom: Moment): Long = if (mom.year() >= 1) mom.year() else 1 - mom.year()

    private fun hFormat(mom: Moment): Long = (mom.hours() % 12).let { if (it == 0) 12 else it }.toLong()

    private fun offset(mom: Moment, separator: String): String {
        var offset = mom.utcOffset
        var sign = "+"
        if (offset < 0) {
            offset = -offset
            sign = "-"
        }
        return sign + zeroFill((offset / 60).toLong(), 2) + separator + zeroFill((offset % 60).toLong(), 2)
    }

    private fun formatToken(mom: Moment, token: String): String = when (token) {
        "M" -> (mom.month() + 1).toString()
        "MM" -> zeroFill(mom.month() + 1L, 2)
        "Mo" -> ordinal(mom.month() + 1L)
        "MMM" -> MONTHS_SHORT[mom.month()]
        "MMMM" -> MONTHS[mom.month()]
        "Q" -> mom.quarter().toString()
        "Qo" -> ordinal(mom.quarter().toLong())
        "D" -> mom.date().toString()
        "DD" -> zeroFill(mom.date().toLong(), 2)
        "Do" -> ordinal(mom.date().toLong())
        "DDD" -> mom.dayOfYear().toString()
        "DDDD" -> zeroFill(mom.dayOfYear().toLong(), 3)
        "DDDo" -> ordinal(mom.dayOfYear().toLong())
        "d" -> mom.day().toString()
        "do" -> ordinal(mom.day().toLong())
        "dd" -> WEEKDAYS_MIN[mom.day()]
        "ddd" -> WEEKDAYS_SHORT[mom.day()]
        "dddd" -> WEEKDAYS[mom.day()]
        "e" -> mom.weekdayEn().toString()
        "E" -> mom.isoWeekday().toString()
        "w" -> mom.weekEn().toString()
        "ww" -> zeroFill(mom.weekEn().toLong(), 2)
        "wo" -> ordinal(mom.weekEn().toLong())
        "W" -> mom.isoWeek().toString()
        "WW" -> zeroFill(mom.isoWeek().toLong(), 2)
        "Wo" -> ordinal(mom.isoWeek().toLong())
        "Y" -> mom.year().let { if (it <= 9999) zeroFill(it, 4) else "+$it" }
        "YY" -> zeroFill(mom.year() % 100, 2)
        "YYYY" -> zeroFill(mom.year(), 4)
        "YYYYY" -> zeroFill(mom.year(), 5)
        "YYYYYY" -> zeroFill(mom.year(), 6, true)
        "gg" -> zeroFill(mom.weekYearEn() % 100, 2)
        "gggg" -> zeroFill(mom.weekYearEn(), 4)
        "ggggg" -> zeroFill(mom.weekYearEn(), 5)
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
        "a" -> if (mom.hours() > 11) "pm" else "am"
        "A" -> if (mom.hours() > 11) "PM" else "AM"
        "m" -> mom.minutes().toString()
        "mm" -> zeroFill(mom.minutes().toLong(), 2)
        "s" -> mom.seconds().toString()
        "ss" -> zeroFill(mom.seconds().toLong(), 2)
        "S" -> (mom.milliseconds() / 100).toString()
        "SS" -> zeroFill((mom.milliseconds() / 10).toLong(), 2)
        "SSS" -> zeroFill(mom.milliseconds().toLong(), 3)
        "Z" -> offset(mom, ":")
        "ZZ" -> offset(mom, "")
        "z", "zz" -> mom.zoneAbbr()
        "X" -> floor(mom.valueOf / 1000.0).toLong().toString()
        "x" -> mom.valueOf.toString()
        "N", "NN", "NNN", "NNNNN" -> if (mom.year() >= 1) "AD" else "BC"
        "NNNN" -> if (mom.year() >= 1) "Anno Domini" else "Before Christ"
        "y" -> zeroFill(eraYear(mom), 1)
        "yy" -> zeroFill(eraYear(mom), 2)
        "yyy" -> zeroFill(eraYear(mom), 3)
        "yyyy" -> zeroFill(eraYear(mom), 4)
        "yo" -> ordinal(eraYear(mom))
        else -> {
            // SSSS .. SSSSSSSSS
            val multiplier = "1" + "0".repeat(token.length - 3)
            zeroFill(mom.milliseconds() * multiplier.toLong(), token.length)
        }
    }
}
