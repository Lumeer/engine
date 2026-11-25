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

import io.lumeer.datafilters.util.jsString
import io.lumeer.datafilters.util.stringToJsNumber
import kotlin.math.abs
import kotlin.math.floor

/** Port of moment.js string parsing (non-strict mode, "en" locale). */
internal object MomentParse {

    private const val YEAR = 0
    private const val MONTH = 1
    private const val DATE = 2
    private const val HOUR = 3
    private const val MINUTE = 4
    private const val SECOND = 5
    private const val MILLISECOND = 6
    private const val WEEK = 7
    private const val WEEKDAY = 8

    private val leadingInteger = Regex("^\\s*([+-]?\\d+)")
    private val offsetChunks = Regex("([+\\-]|\\d\\d)", RegexOption.IGNORE_CASE)
    private val regexSpecial = Regex("[-/\\\\^$*+?.()|\\[\\]{}]")
    private val formatEscapes = Regex("\\\\(\\[)|\\\\(\\])|\\[([^\\]\\[]*)\\]|\\\\(.)")
    private val aspNetJsonRegex = Regex("^/?Date\\((-?\\d+)", RegexOption.IGNORE_CASE)

    const val ISO_8601 = "\\u0000ISO_8601"

    private val match1 = Regex("\\d")
    private val match3 = Regex("\\d{3}")
    private val match1to2 = Regex("\\d\\d?")
    private val match3to4 = Regex("\\d\\d\\d\\d?")
    private val match5to6 = Regex("\\d\\d\\d\\d\\d\\d?")
    private val match1to3 = Regex("\\d{1,3}")
    private val match1to4 = Regex("\\d{1,4}")
    private val match1to6 = Regex("[+-]?\\d{1,6}")
    private val matchUnsigned = Regex("\\d+")
    private val matchSigned = Regex("[+-]?\\d+")
    private val matchShortOffset = Regex("Z|[+-]\\d\\d(?::?\\d\\d)?", RegexOption.IGNORE_CASE)
    private val matchTimestamp = Regex("[+-]?\\d+(\\.\\d{1,3})?")
    private val meridiemParse = Regex("[ap]\\.?m?\\.?", RegexOption.IGNORE_CASE)
    // moment's global locale in the original bundle is "fr" (the last imported locale), parsing uses it
    private val ordinalLenient = Regex("\\d{1,2}(er|)|\\d{1,2}")
    private val frMonthsRegex = Regex("(janv\\.?|févr\\.?|mars|avr\\.?|mai|juin|juil\\.?|août|sept\\.?|oct\\.?|nov\\.?|déc\\.?|janvier|février|mars|avril|mai|juin|juillet|août|septembre|octobre|novembre|décembre)", RegexOption.IGNORE_CASE)
    private val frMonthsParse = listOf("^janv", "^févr", "^mars", "^avr", "^mai", "^juin", "^juil", "^août", "^sept", "^oct", "^nov", "^déc").map { Regex(it, RegexOption.IGNORE_CASE) }
    private val frWeekdays = listOf("dimanche", "lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi")
    private val frWeekdaysShort = listOf("dim.", "lun.", "mar.", "mer.", "jeu.", "ven.", "sam.")
    private val frWeekdaysMin = listOf("di", "lu", "ma", "me", "je", "ve", "sa")
    private val frWeekdaysRegex: Regex = run {
        val pieces = (0 until 7).flatMap { listOf(frWeekdaysMin[it], frWeekdaysShort[it], frWeekdays[it]) }.map { regexEscape(it) }
        Regex("^(" + pieces.sortedByDescending { it.length }.joinToString("|") + ")", RegexOption.IGNORE_CASE)
    }
    private val frLongDateFormats = mapOf("LT" to "HH:mm", "LTS" to "HH:mm:ss", "L" to "DD/MM/YYYY", "LL" to "D MMMM YYYY", "LLL" to "D MMMM YYYY HH:mm", "LLLL" to "dddd D MMMM YYYY HH:mm")
    private val localFormattingTokens = Regex("(\\[[^\\[]*\\])|(\\\\)?(LTS|LT|LL?L?L?|l{1,4})")

    /** expandFormat with the fr long date formats */
    private fun expandFormatFr(format: String): String {
        var result = format
        var i = 5
        while (i >= 0 && localFormattingTokens.containsMatchIn(result)) {
            result = result.replace(localFormattingTokens) { match ->
                val key = match.value
                frLongDateFormats[key] ?: frLongDateFormats[key.uppercase()]?.let { upper ->
                    MomentFormat.FORMATTING_TOKENS.findAll(upper).joinToString("") { t ->
                        if (t.value == "MMMM" || t.value == "MM" || t.value == "DD" || t.value == "dddd") t.value.substring(1) else t.value
                    }
                } ?: key
            }
            i -= 1
        }
        return result
    }
    private val eraAbbr = Regex("^(AD|BC)", RegexOption.IGNORE_CASE)
    private val eraName = Regex("^(Anno Domini|Before Christ)", RegexOption.IGNORE_CASE)

    private fun regexForToken(token: String): Regex? = when (token) {
        "Y", "x", "G", "g" -> matchSigned
        "YY", "M", "MM", "D", "DD", "d", "e", "E", "w", "ww", "W", "WW", "GG", "gg", "H", "h", "k", "HH", "hh", "kk", "m", "mm", "s", "ss" -> match1to2
        "YYYY", "GGGG", "gggg" -> match1to4
        "YYYYY", "YYYYYY", "GGGGG", "ggggg" -> match1to6
        "MMM", "MMMM" -> frMonthsRegex
        "dd", "ddd", "dddd" -> frWeekdaysRegex
        "Do" -> ordinalLenient
        "DDD", "S", "SS", "SSS" -> match1to3
        "DDDD" -> match3
        "Q" -> match1
        "hmm", "Hmm" -> match3to4
        "hmmss", "Hmmss" -> match5to6
        "a", "A" -> meridiemParse
        "X" -> matchTimestamp
        "Z", "ZZ" -> matchShortOffset
        "y", "yy", "yyy", "yyyy" -> matchUnsigned
        "N", "NN", "NNN", "NNNNN" -> eraAbbr
        "NNNN" -> eraName
        else -> if (token.length in 4..9 && token.all { it == 'S' }) matchUnsigned else null
    }

    private class Flags {
        var empty = false
        val unusedTokens = mutableListOf<String>()
        var charsLeftOver = 0
        var overflow = -2
        var invalidMonth = false
        var invalidWeekday = false
        var weekdayMismatch = false
        var invalidEra = false
        var meridiem: String? = null
        var parsedDateParts = false
        var overflowDayOfYear = false
        var overflowWeeks = false
        var overflowWeekday = false
        var era: String? = null
    }

    private class Config(val input: String, val format: String, val isUtc: Boolean, val ctx: TimeContext) {
        val a = arrayOfNulls<Double>(7)
        var w: MutableMap<String, Double>? = null
        var dayOfYear: Double? = null
        var tzm: Double? = null
        var useUTC = isUtc
        var meridiem: String? = null
        var directDate: Double? = null
        var nextDay = false
        var isValidOverride: Boolean? = null
        var flags = Flags()

        /** UTC millis of the parsed date (config._d), NaN when invalid */
        var dateMillis: Double? = null
        var fromIsoOrRfcInstant: Double? = null
    }

    // ---- helpers

    private fun toInt(input: Any?): Double {
        val number = when (input) {
            is Double -> input
            is String -> stringToJsNumber(input)
            else -> Double.NaN
        }
        if (number != 0.0 && !number.isNaN() && !number.isInfinite()) {
            return if (number < 0) -floor(-number) else floor(number)
        }
        return 0.0
    }

    private fun parseTwoDigitYear(input: String): Double {
        val value = toInt(input)
        return value + (if (value > 68) 1900 else 2000)
    }

    /** JS parseInt(input, 10) for the digit strings produced by the token regexes */
    private fun parseIntJs(input: String): Double {
        val match = leadingInteger.find(input) ?: return Double.NaN
        return match.groupValues[1].toDouble()
    }

    private fun monthsParse(input: String): Int? {
        for (i in 0 until 12) {
            if (frMonthsParse[i].containsMatchIn(input)) return i
        }
        return null
    }

    /** weekdaysParseExact handleStrictParse, non-strict */
    private fun weekdaysParse(input: String, token: String): Int? {
        val llc = input.lowercase()
        val order = when (token) {
            "dddd" -> listOf(frWeekdays, frWeekdaysShort, frWeekdaysMin)
            "ddd" -> listOf(frWeekdaysShort, frWeekdays, frWeekdaysMin)
            else -> listOf(frWeekdaysMin, frWeekdays, frWeekdaysShort)
        }
        for (list in order) {
            val index = list.indexOf(llc)
            if (index != -1) return index
        }
        return null
    }

    private fun offsetFromString(input: String): Double? {
        val matches = matchShortOffset.findAll(input).map { it.value }.toList()
        if (matches.isEmpty()) return null
        val chunk = matches.last()
        val parts = offsetChunks.findAll(chunk).map { it.value }.toList().ifEmpty { listOf("-", "0", "0") }
        val minutes = stringToJsNumber(parts.getOrElse(1) { "0" }) * 60 + toInt(parts.getOrNull(2))
        return if (minutes == 0.0) 0.0 else if (parts[0] == "+") minutes else -minutes
    }

    private fun regexEscape(s: String): String = s.replace(regexSpecial) { "\\" + it.value }

    private fun unescapeFormat(s: String): String = regexEscape(
        s.replaceFirst("\\", "").replace(formatEscapes) { m ->
            m.groupValues[1].ifEmpty { m.groupValues[2].ifEmpty { m.groupValues[3].ifEmpty { m.groupValues[4] } } }
        }
    )

    // ---- token handlers (parse/token.js)

    private fun addTimeToArrayFromToken(token: String, input: String, config: Config) {
        val a = config.a
        fun week(key: String, value: Double) {
            val w = config.w ?: mutableMapOf<String, Double>().also { config.w = it }
            w[key] = value
        }
        when (token) {
            "YYYYY", "YYYYYY" -> a[YEAR] = toInt(input)
            "YYYY" -> a[YEAR] = if (input.length == 2) parseTwoDigitYear(input) else toInt(input)
            "YY" -> a[YEAR] = parseTwoDigitYear(input)
            "Y" -> a[YEAR] = parseIntJs(input)
            "M", "MM" -> a[MONTH] = toInt(input) - 1
            "MMM", "MMMM" -> {
                val month = monthsParse(input)
                if (month != null) a[MONTH] = month.toDouble() else config.flags.invalidMonth = true
            }
            "Q" -> a[MONTH] = (toInt(input) - 1) * 3
            "D", "DD" -> a[DATE] = toInt(input)
            "Do" -> a[DATE] = toInt(match1to2.find(input)!!.value)
            "DDD", "DDDD" -> config.dayOfYear = toInt(input)
            "dd", "ddd", "dddd" -> {
                val weekday = weekdaysParse(input, token)
                if (weekday != null) week("d", weekday.toDouble()) else config.flags.invalidWeekday = true
            }
            "d", "e", "E" -> week(token, toInt(input))
            "w", "ww", "W", "WW" -> week(token.substring(0, 1), toInt(input))
            "gggg", "ggggg", "GGGG", "GGGGG" -> week(token.substring(0, 2), toInt(input))
            "gg", "GG" -> week(token, parseTwoDigitYear(input))
            "H", "HH" -> a[HOUR] = toInt(input)
            "k", "kk" -> a[HOUR] = toInt(input).let { if (it == 24.0) 0.0 else it }
            "a", "A" -> config.meridiem = input
            "h", "hh" -> a[HOUR] = toInt(input)
            "hmm", "Hmm" -> {
                val pos = input.length - 2
                a[HOUR] = toInt(input.substring(0, pos))
                a[MINUTE] = toInt(input.substring(pos))
            }
            "hmmss", "Hmmss" -> {
                val pos1 = input.length - 4
                val pos2 = input.length - 2
                a[HOUR] = toInt(input.substring(0, pos1))
                a[MINUTE] = toInt(input.substring(pos1, pos1 + 2))
                a[SECOND] = toInt(input.substring(pos2))
            }
            "m", "mm" -> a[MINUTE] = toInt(input)
            "s", "ss" -> a[SECOND] = toInt(input)
            "X" -> config.directDate = stringToJsNumber(input) * 1000
            "x" -> config.directDate = toInt(input)
            "Z", "ZZ" -> {
                config.useUTC = true
                config.tzm = offsetFromString(input)
            }
            "y", "yy", "yyy", "yyyy" -> a[YEAR] = toInt(input)
            "yo" -> a[YEAR] = parseIntJs(input)
            "N", "NN", "NNN", "NNNN", "NNNNN" -> {
                val upper = input.uppercase()
                when (upper) {
                    "AD", "ANNO DOMINI" -> config.flags.era = "AD"
                    "BC", "BEFORE CHRIST" -> config.flags.era = "BC"
                    else -> config.flags.invalidEra = true
                }
            }
            else -> if (token.all { it == 'S' }) {
                a[MILLISECOND] = toInt(stringToJsNumber("0.$input") * 1000)
            }
        }
    }

    // ---- from-string-and-format.js

    private fun configFromStringAndFormat(config: Config, format: String) {
        if (format == ISO_8601) {
            configFromISO(config)
            return
        }
        config.flags.empty = true
        var string = config.input
        val stringLength = string.length
        var totalParsedInputLength = 0

        val tokens = MomentFormat.FORMATTING_TOKENS.findAll(expandFormatFr(format)).map { it.value }.toList()
        for (token in tokens) {
            val regex = regexForToken(token) ?: Regex(unescapeFormat(token))
            val parsedInput = regex.find(string)?.value
            if (parsedInput != null && parsedInput.isNotEmpty()) {
                val index = string.indexOf(parsedInput)
                string = string.substring(index + parsedInput.length)
                totalParsedInputLength += parsedInput.length
            }
            if (token in MomentFormat.FORMAT_TOKENS) {
                if (!parsedInput.isNullOrEmpty()) {
                    config.flags.empty = false
                } else {
                    config.flags.unusedTokens.add(token)
                }
                if (parsedInput != null) {
                    addTimeToArrayFromToken(token, parsedInput, config)
                }
            }
        }

        config.flags.charsLeftOver = stringLength - totalParsedInputLength
        config.flags.parsedDateParts = config.a.any { it != null }
        config.flags.meridiem = config.meridiem

        // handle meridiem
        val meridiem = config.meridiem
        val hour = config.a[HOUR]
        if (meridiem != null && hour != null) {
            val isPm = meridiem.lowercase().startsWith("p")
            if (isPm && hour < 12) config.a[HOUR] = hour + 12
            if (!isPm && hour == 12.0) config.a[HOUR] = 0.0
        }

        // handle era
        when (config.flags.era) {
            "AD" -> config.a[YEAR] = config.a[YEAR]?.let { 1 + (it - 1) } ?: 1.0
            "BC" -> config.a[YEAR] = config.a[YEAR]?.let { 0 - (it - 1) } ?: 0.0
        }

        configFromArray(config)
        checkOverflow(config)
    }

    // ---- from-array.js

    private fun configFromArray(config: Config) {
        if (config.directDate != null) {
            config.dateMillis = config.directDate
            return
        }
        val now = config.ctx.nowMillis
        val currentFields = if (config.useUTC) Fields(now) else Fields(now + TimeZones.offsetMillis(config.ctx.systemZone, now))
        val currentDate = doubleArrayOf(currentFields.year.toDouble(), currentFields.month0.toDouble(), currentFields.date.toDouble())

        if (config.w != null && config.a[DATE] == null && config.a[MONTH] == null) {
            dayOfYearFromWeekInfo(config)
        }

        val dayOfYear = config.dayOfYear
        if (dayOfYear != null) {
            val yearToUse = config.a[YEAR] ?: currentDate[0]
            if (dayOfYear > Civil.daysInYear(yearToUse.toLong()) || dayOfYear == 0.0) {
                config.flags.overflowDayOfYear = true
            }
            val f = Fields(Civil.fieldsToMillis(yearToUse.toLong(), 0, dayOfYear.toLong()))
            config.a[MONTH] = f.month0.toDouble()
            config.a[DATE] = f.date.toDouble()
        }

        var i = 0
        while (i < 3 && config.a[i] == null) {
            config.a[i] = currentDate[i]
            i++
        }
        while (i < 7) {
            if (config.a[i] == null) config.a[i] = if (i == 2) 1.0 else 0.0
            i++
        }

        val a = config.a.map { it!! }
        if (a[HOUR] == 24.0 && a[MINUTE] == 0.0 && a[SECOND] == 0.0 && a[MILLISECOND] == 0.0) {
            config.nextDay = true
            config.a[HOUR] = 0.0
        }
        val values = config.a.map { it!! }
        if (values.any { it.isNaN() || abs(it) > 1e15 }) {
            config.dateMillis = Double.NaN
            return
        }
        val wall = Civil.fieldsToMillis(values[0].toLong(), values[1].toLong(), values[2].toLong(), values[3].toLong(), values[4].toLong(), values[5].toLong(), values[6].toLong())
        val expectedWeekday = Fields(wall).dayOfWeek
        config.dateMillis = wall.toDouble()
        if (config.nextDay) {
            config.a[HOUR] = 24.0
        }

        val w = config.w
        val d = w?.get("d")
        if (w != null && d != null && d != expectedWeekday.toDouble()) {
            config.flags.weekdayMismatch = true
        }
    }

    private fun dayOfYearFromWeekInfo(config: Config) {
        val w = config.w!!
        val weekYear: Double
        val week: Double
        val weekday: Double
        val dow: Int
        val doy: Int
        var weekdayOverflow = false
        if (w["GG"] != null || w["W"] != null || w["E"] != null) {
            dow = 1
            doy = 4
            weekYear = w["GG"] ?: config.a[YEAR] ?: Moment.weekOfYear(Moment.now(config.ctx, false), 1, 4).second.toDouble()
            week = w["W"] ?: 1.0
            weekday = w["E"] ?: 1.0
            if (weekday < 1 || weekday > 7) weekdayOverflow = true
        } else {
            dow = Moment.LOCALE_DOW
            doy = Moment.LOCALE_DOY
            val curWeek = Moment.weekOfYear(Moment.now(config.ctx, false), dow, doy)
            weekYear = w["gg"] ?: config.a[YEAR] ?: curWeek.second.toDouble()
            week = w["w"] ?: curWeek.first.toDouble()
            val d = w["d"]
            val e = w["e"]
            if (d != null) {
                weekday = d
                if (d < 0 || d > 6) weekdayOverflow = true
            } else if (e != null) {
                weekday = e + dow
                if (e < 0 || e > 6) weekdayOverflow = true
            } else {
                weekday = dow.toDouble()
            }
        }
        if (week < 1 || week > Moment.weeksInYear(weekYear.toLong(), dow, doy)) {
            config.flags.overflowWeeks = true
        } else if (weekdayOverflow) {
            config.flags.overflowWeekday = true
        } else {
            val (year, dayOfYear) = Moment.dayOfYearFromWeeks(weekYear.toLong(), week.toInt(), weekday.toInt(), dow, doy)
            config.a[YEAR] = year.toDouble()
            config.dayOfYear = dayOfYear.toDouble()
        }
    }

    private fun checkOverflow(config: Config) {
        val a = config.a
        if (config.flags.overflow == -2 && a.all { it != null }) {
            val v = a.map { it!! }
            var overflow = when {
                v[MONTH] < 0 || v[MONTH] > 11 -> MONTH
                v[DATE] < 1 || v[DATE] > Civil.daysInMonth(v[YEAR].toLong(), v[MONTH].toLong()) -> DATE
                v[HOUR] < 0 || v[HOUR] > 24 || (v[HOUR] == 24.0 && (v[MINUTE] != 0.0 || v[SECOND] != 0.0 || v[MILLISECOND] != 0.0)) -> HOUR
                v[MINUTE] < 0 || v[MINUTE] > 59 -> MINUTE
                v[SECOND] < 0 || v[SECOND] > 59 -> SECOND
                v[MILLISECOND] < 0 || v[MILLISECOND] > 999 -> MILLISECOND
                else -> -1
            }
            if (config.flags.overflowDayOfYear && (overflow < YEAR || overflow > DATE)) overflow = DATE
            if (config.flags.overflowWeeks && overflow == -1) overflow = WEEK
            if (config.flags.overflowWeekday && overflow == -1) overflow = WEEKDAY
            config.flags.overflow = overflow
        }
    }

    private fun isValid(config: Config): Boolean {
        config.isValidOverride?.let { return it }
        val f = config.flags
        val date = config.dateMillis ?: Double.NaN
        val valid = !date.isNaN() &&
            f.overflow < 0 &&
            !f.empty &&
            !f.invalidEra &&
            !f.invalidMonth &&
            !f.invalidWeekday &&
            !f.weekdayMismatch &&
            (f.meridiem == null || f.parsedDateParts)
        config.isValidOverride = valid
        return valid
    }

    // ---- from-string.js

    private val extendedIsoRegex = Regex("^\\s*((?:[+-]\\d{6}|\\d{4})-(?:\\d\\d-\\d\\d|W\\d\\d-\\d|W\\d\\d|\\d\\d\\d|\\d\\d))(?:(T| )(\\d\\d(?::\\d\\d(?::\\d\\d(?:[.,]\\d+)?)?)?)([+-]\\d\\d(?::?\\d\\d)?|\\s*Z)?)?$")
    private val basicIsoRegex = Regex("^\\s*((?:[+-]\\d{6}|\\d{4})(?:\\d\\d\\d\\d|W\\d\\d\\d|W\\d\\d|\\d\\d\\d|\\d\\d|))(?:(T| )(\\d\\d(?:\\d\\d(?:\\d\\d(?:[.,]\\d+)?)?)?)([+-]\\d\\d(?::?\\d\\d)?|\\s*Z)?)?$")
    private val tzRegex = Regex("Z|[+-]\\d\\d(?::?\\d\\d)?")
    private val isoDates = listOf(
        Triple("YYYYYY-MM-DD", Regex("[+-]\\d{6}-\\d\\d-\\d\\d"), true),
        Triple("YYYY-MM-DD", Regex("\\d{4}-\\d\\d-\\d\\d"), true),
        Triple("GGGG-[W]WW-E", Regex("\\d{4}-W\\d\\d-\\d"), true),
        Triple("GGGG-[W]WW", Regex("\\d{4}-W\\d\\d"), false),
        Triple("YYYY-DDD", Regex("\\d{4}-\\d{3}"), true),
        Triple("YYYY-MM", Regex("\\d{4}-\\d\\d"), false),
        Triple("YYYYYYMMDD", Regex("[+-]\\d{10}"), true),
        Triple("YYYYMMDD", Regex("\\d{8}"), true),
        Triple("GGGG[W]WWE", Regex("\\d{4}W\\d{3}"), true),
        Triple("GGGG[W]WW", Regex("\\d{4}W\\d{2}"), false),
        Triple("YYYYDDD", Regex("\\d{7}"), true),
        Triple("YYYYMM", Regex("\\d{6}"), false),
        Triple("YYYY", Regex("\\d{4}"), false),
    )
    private val isoTimes = listOf(
        "HH:mm:ss.SSSS" to Regex("\\d\\d:\\d\\d:\\d\\d\\.\\d+"),
        "HH:mm:ss,SSSS" to Regex("\\d\\d:\\d\\d:\\d\\d,\\d+"),
        "HH:mm:ss" to Regex("\\d\\d:\\d\\d:\\d\\d"),
        "HH:mm" to Regex("\\d\\d:\\d\\d"),
        "HHmmss.SSSS" to Regex("\\d\\d\\d\\d\\d\\d\\.\\d+"),
        "HHmmss,SSSS" to Regex("\\d\\d\\d\\d\\d\\d,\\d+"),
        "HHmmss" to Regex("\\d\\d\\d\\d\\d\\d"),
        "HHmm" to Regex("\\d\\d\\d\\d"),
        "HH" to Regex("\\d\\d"),
    )

    private fun configFromISO(config: Config) {
        val match = extendedIsoRegex.find(config.input) ?: basicIsoRegex.find(config.input)
        if (match == null) {
            config.isValidOverride = false
            return
        }
        val datePart = match.groupValues[1]
        val dateEntry = isoDates.firstOrNull { it.second.containsMatchIn(datePart) }
        if (dateEntry == null) {
            config.isValidOverride = false
            return
        }
        var timeFormat: String? = null
        if (match.groupValues[3].isNotEmpty()) {
            val timeEntry = isoTimes.firstOrNull { it.second.containsMatchIn(match.groupValues[3]) }
            if (timeEntry == null) {
                config.isValidOverride = false
                return
            }
            timeFormat = match.groupValues[2].ifEmpty { " " } + timeEntry.first
        }
        if (!dateEntry.third && timeFormat != null) {
            config.isValidOverride = false
            return
        }
        var tzFormat = ""
        if (match.groupValues[4].isNotEmpty()) {
            if (tzRegex.containsMatchIn(match.groupValues[4])) {
                tzFormat = "Z"
            } else {
                config.isValidOverride = false
                return
            }
        }
        configFromStringAndFormat(config, dateEntry.first + (timeFormat ?: "") + tzFormat)
    }

    private val rfc2822 = Regex("^(?:(Mon|Tue|Wed|Thu|Fri|Sat|Sun),?\\s)?(\\d{1,2})\\s(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\s(\\d{2,4})\\s(\\d\\d):(\\d\\d)(?::(\\d\\d))?\\s(?:(UT|GMT|[ECMP][SD]T)|([Zz])|([+-]\\d{4}))$")
    private val obsOffsets = mapOf("UT" to 0, "GMT" to 0, "EDT" to -240, "EST" to -300, "CDT" to -300, "CST" to -360, "MDT" to -360, "MST" to -420, "PDT" to -420, "PST" to -480)

    /** returns the instant or null when the input is not RFC 2822 */
    private fun parseRfc2822(input: String): Double? {
        val prepared = input.replace(Regex("\\([^()]*\\)|[\\n\\t]"), " ").replace(Regex("(\\s\\s+)"), " ").replace(Regex("^\\s\\s*"), "").replace(Regex("\\s\\s*$"), "")
        val m = rfc2822.find(prepared) ?: return null
        val yearRaw = m.groupValues[4].toInt()
        val year = if (yearRaw <= 49) 2000 + yearRaw else if (yearRaw <= 999) 1900 + yearRaw else yearRaw
        val month = MomentFormat.MONTHS_SHORT.indexOf(m.groupValues[3])
        val day = m.groupValues[2].toInt()
        val wall = Civil.fieldsToMillis(year.toLong(), month.toLong(), day.toLong(), m.groupValues[5].toLong(), m.groupValues[6].toLong(), m.groupValues[7].ifEmpty { "0" }.toLong())
        if (m.groupValues[1].isNotEmpty() && MomentFormat.WEEKDAYS_SHORT.indexOf(m.groupValues[1]) != Fields(wall).dayOfWeek) {
            return Double.NaN
        }
        val offset = when {
            m.groupValues[8].isNotEmpty() -> obsOffsets[m.groupValues[8]]!!
            m.groupValues[9].isNotEmpty() -> 0
            else -> {
                val hm = m.groupValues[10].toInt()
                val minutes = hm % 100
                (hm - minutes) / 100 * 60 + minutes
            }
        }
        return (wall - offset * 60_000L).toDouble()
    }

    private val esDateTime = Regex("^([+-]\\d{6}|\\d{4})(?:-(\\d\\d)(?:-(\\d\\d))?)?(?:T(\\d\\d):(\\d\\d)(?::(\\d\\d)(?:\\.(\\d+))?)?(Z|[+-]\\d\\d:\\d\\d)?)?$")

    /** new Date(string) fallback, limited to the ECMAScript date time string format */
    private fun parseJsDate(input: String, ctx: TimeContext): Double {
        val m = esDateTime.find(input) ?: return Double.NaN
        val g = m.groupValues
        val ms = if (g[7].isEmpty()) 0L else (g[7] + "00").substring(0, 3).toLong()
        val wall = Civil.fieldsToMillis(g[1].toLong(), (g[2].ifEmpty { "1" }.toLong()) - 1, g[3].ifEmpty { "1" }.toLong(), g[4].ifEmpty { "0" }.toLong(), g[5].ifEmpty { "0" }.toLong(), g[6].ifEmpty { "0" }.toLong(), ms)
        return when {
            g[8] == "Z" || g[4].isEmpty() -> wall.toDouble()
            g[8].isNotEmpty() -> {
                val sign = if (g[8][0] == '-') -1 else 1
                (wall - sign * (g[8].substring(1, 3).toLong() * 60 + g[8].substring(4, 6).toLong()) * 60_000L).toDouble()
            }
            else -> TimeZones.wallToEpochMillis(ctx.systemZone, wall).toDouble()
        }
    }

    // ---- moment creation

    private fun createMoment(config: Config): Moment {
        if (!isValid(config)) {
            return Moment.invalid(config.ctx, config.isUtc)
        }
        config.fromIsoOrRfcInstant?.let { return checkRange(config, it) { Moment.fromInstant(config.ctx, config.isUtc, it.toLong()) } }
        val date = config.dateMillis!!
        val mom = when {
            config.directDate != null -> checkRange(config, date) { Moment.fromInstant(config.ctx, config.isUtc, date.toLong()) }
            config.tzm != null || config.useUTC -> {
                val instant = date - (config.tzm ?: 0.0) * 60_000.0
                checkRange(config, instant) { Moment.fromInstant(config.ctx, config.isUtc, instant.toLong()) }
            }
            else -> checkRange(config, date) { Moment.fromWall(config.ctx, config.isUtc, date.toLong()) }
        }
        if (config.nextDay && mom.isValid) {
            mom.addDays(1)
        }
        return mom
    }

    private inline fun checkRange(config: Config, millis: Double, create: () -> Moment): Moment =
        if (millis.isNaN() || abs(millis) > 8.64e15) Moment.invalid(config.ctx, config.isUtc) else create()

    /** moment(value) / moment.utc(value) for a string without format */
    fun parse(input: String, utc: Boolean, ctx: TimeContext): Moment {
        val aspNet = aspNetJsonRegex.find(input)
        if (aspNet != null) {
            return Moment.fromInstant(ctx, utc, aspNet.groupValues[1].toLong())
        }
        val config = Config(input, ISO_8601, utc, ctx)
        configFromISO(config)
        if (config.isValidOverride != false) {
            return createMoment(config)
        }
        val rfc = parseRfc2822(input)
        if (rfc != null) {
            return if (rfc.isNaN()) Moment.invalid(ctx, utc) else Moment.fromInstant(ctx, utc, rfc.toLong())
        }
        val jsDate = parseJsDate(input, ctx)
        return if (jsDate.isNaN()) Moment.invalid(ctx, utc) else Moment.fromInstant(ctx, utc, jsDate.toLong())
    }

    /** moment(value, [formats]) / moment.utc(value, [formats]) */
    fun parse(value: Any?, formats: List<String>, utc: Boolean, ctx: TimeContext): Moment {
        val input = if (value is List<*> && value.isEmpty() || value is Array<*> && value.isEmpty()) "undefined" else jsString(value)
        if (formats.isEmpty()) {
            return Moment.invalid(ctx, utc)
        }
        var bestConfig: Config? = null
        var lastConfig: Config? = null
        var scoreToBeat: Int? = null
        var bestFormatIsValid = false
        for (format in formats) {
            val config = Config(input, format, utc, ctx)
            configFromStringAndFormat(config, format)
            lastConfig = config
            val validFormatFound = isValid(config)
            val score = config.flags.charsLeftOver + config.flags.unusedTokens.size * 10
            if (!bestFormatIsValid) {
                if (scoreToBeat == null || score < scoreToBeat || validFormatFound) {
                    scoreToBeat = score
                    bestConfig = config
                    if (validFormatFound) bestFormatIsValid = true
                }
            } else if (score < scoreToBeat!!) {
                scoreToBeat = score
                bestConfig = config
            }
        }
        return createMoment(bestConfig ?: lastConfig!!)
    }
}
