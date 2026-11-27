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

import io.lumeer.datafilters.date.DateParser
import io.lumeer.datafilters.date.TimeContext
import io.lumeer.datafilters.util.jsString
import kotlin.math.abs
import kotlin.math.floor

/**
 * The parts of moment 2.31 (with the "en" global locale) that the automation blocks use, as they run in the engine:
 * moments in local time of [zone], backed by a JS Date time value.
 */
internal class ScriptDateFactory(
    val zone: String,
    private val now: () -> Long,
    private val jsDateParse: ((String) -> Double)?,
) {
    val math = JsDateMath(zone)

    /** moment(input) */
    fun create(input: Any?): ScriptDate {
        var value = input
        if ((value is Map<*, *> && value.isEmpty()) || (value is List<*> && value.isEmpty())) {
            value = JsUndefined
        }
        if (value == null || value == "") {
            return ScriptDate(this, Double.NaN, false)
        }
        val time = when (value) {
            is JsDate -> value.time
            JsUndefined -> now().toDouble()
            is String -> fromString(value)
            is Number -> JsDateMath.timeClip(value.toDouble())
            is List<*>, is Map<*, *> -> throw AutomationFunctionException("Dates from arrays and objects are not supported")
            // createFromInputFallback: new Date(input + '')
            else -> parseJsDate(jsString(value))
        }
        return ScriptDate(this, time, !time.isNaN())
    }

    private fun parseJsDate(input: String): Double =
        jsDateParse?.invoke(input) ?: Double.NaN

    private fun fromString(input: String): Double {
        val ctx = TimeContext(null, now(), zone)
        val mom = DateParser.parse(input, false, ctx, jsDateParse, latest = true)
        return if (mom.isValid) mom.valueOf.toDouble() else Double.NaN
    }
}

internal class ScriptDate(private val factory: ScriptDateFactory, var d: Double, private val valid: Boolean) {

    private val math get() = factory.math

    fun isValid(): Boolean = valid

    fun clone(): ScriptDate = ScriptDate(factory, d, valid)

    fun valueOf(): Double = d

    // ---- getters

    fun year(): Double = if (valid) math.getFullYear(d) else Double.NaN
    fun month(): Double = if (valid) math.getMonth(d) else Double.NaN
    fun date(): Double = if (valid) math.getDate(d) else Double.NaN
    fun day(): Double = if (valid) math.getDay(d) else Double.NaN

    /** locale weekday, "en": Sunday = 0 */
    private fun weekday(): Double = mod(day() + 7 - DOW, 7.0)

    private fun utcOffset(): Double = -jsRound(math.getTimezoneOffset(d))

    fun daysInMonth(): Double = daysInMonth(year(), month())

    // ---- set$1 and the setters made by makeGetSet

    private fun set(unit: String, value: Any?) {
        if (valid && !jsNumber(value).isNaN()) {
            d = when (unit) {
                "FullYear" -> {
                    val month = month()
                    val date = date().let { if (it == 29.0 && month == 1.0 && !isLeapYear(value)) 28.0 else it }
                    math.setFullYear(d, value, month, date)
                }
                "Date" -> math.setDate(d, value)
                "Hours" -> math.setHours(d, value)
                "Minutes" -> math.setMinutes(d, value)
                "Seconds" -> math.setSeconds(d, value)
                else -> error(unit)
            }
        }
    }

    /** a getter/setter called with null or undefined returns a number, and calling a moment method on it is a TypeError */
    private fun requireValue(value: Any?, name: String) {
        if (value == null || value === JsUndefined) {
            throw AutomationFunctionException("moment().$name() without a value returns a number")
        }
    }

    fun setDate(value: Any?) = apply { requireValue(value, "date"); set("Date", value) }
    fun setHour(value: Any?) = apply { requireValue(value, "hour"); set("Hours", value) }
    fun setMinute(value: Any?) = apply { requireValue(value, "minute"); set("Minutes", value) }
    fun setSecond(value: Any?) = apply { requireValue(value, "second"); set("Seconds", value) }
    fun setYear(value: Any?) = apply { requireValue(value, "year"); set("FullYear", value) }

    /** getSetMonth */
    fun setMonth(value: Any?) = apply { requireValue(value, "month"); setMonthInternal(value) }

    private fun setMonthInternal(input: Any?) {
        if (!valid) return
        var value = input
        if (value is String) {
            value = if (DIGITS.matches(value)) toInt(value) else (monthsParse(value) ?: return)
        }
        val date = date()
        val dayOfMonth = if (date < 29) date else jsMin(date, daysInMonth(year(), value))
        d = math.setMonth(d, value, dayOfMonth)
    }

    /** getSetQuarter */
    fun setQuarter(value: Any?) = apply {
        requireValue(value, "quarter")
        setMonthInternal((jsNumber(value) - 1) * 3 + month() % 3)
    }

    /** getSetWeek with the "en" week (dow 0, doy 6) */
    fun setWeek(value: Any?) = apply {
        requireValue(value, "week")
        add((jsNumber(value) - week()) * 7, "d")
    }

    private fun week(): Double {
        val year = year()
        if (year.isNaN()) return Double.NaN
        val weekOffset = firstWeekOffset(year)
        val dayOfYear = jsRound((clone().startOf("day").d - clone().startOf("year").d) / JsDateMath.MS_PER_DAY) + 1
        val week = floor((dayOfYear - weekOffset - 1) / 7) + 1
        return when {
            week < 1 -> week + weeksInYear(year - 1)
            week > weeksInYear(year) -> week - weeksInYear(year)
            else -> week
        }
    }

    private fun firstWeekOffset(year: Double): Double {
        val fwd = 7 + DOW - DOY
        val fwdlw = mod(7 + utcWeekDay(year, 0.0, fwd.toDouble()) - DOW, 7.0)
        return -fwdlw + fwd - 1
    }

    /** createUTCDate(year, 0, fwd).getUTCDay() */
    private fun utcWeekDay(year: Double, month: Double, date: Double): Double =
        mod(JsDateMath.makeDay(year, month, date) + 4, 7.0)

    private fun weeksInYear(year: Double): Double {
        val weekOffset = firstWeekOffset(year)
        val weekOffsetNext = firstWeekOffset(year + 1)
        return (daysInYear(year) - weekOffset + weekOffsetNext) / 7
    }

    // ---- add / subtract

    fun add(value: Any?, period: String) = apply { addSubtract(value, period, 1) }
    fun subtract(value: Any?, period: String) = apply { addSubtract(value, period, -1) }

    private fun addSubtract(input: Any?, period: String, direction: Int) {
        // createAdder: strings to numbers; createDuration: anything that +x makes a number, NaN counts as 0 (`x || 0`)
        val amount = jsNumber(input).takeUnless { it.isNaN() } ?: 0.0
        var milliseconds = 0.0
        var days = 0.0
        var months = 0.0
        when (normalizeUnit(period)) {
            "year" -> months = amount * 12
            "quarter" -> months = amount * 3
            "month" -> months = amount
            "week" -> days = amount * 7
            "day" -> days = amount
            "hour" -> milliseconds = amount * 1000 * 60 * 60
            "minute" -> milliseconds = amount * 6e4
            "second" -> milliseconds = amount * 1e3
            "millisecond" -> milliseconds = amount
            else -> error(period)
        }
        days = absRound(days)
        months = absRound(months)
        if (!valid) return
        if (months != 0.0 && !months.isNaN()) {
            setMonthInternal(month() + months * direction)
        }
        if (days != 0.0 && !days.isNaN()) {
            set("Date", date() + days * direction)
        }
        if (milliseconds != 0.0 && !milliseconds.isNaN()) {
            d = JsDateMath.timeClip(d + milliseconds * direction)
        }
    }

    // ---- startOf / endOf (moment 2.24)

    private fun startOfDate(y: Double, m: Double, d: Double): Double =
        if (y < 100 && y >= 0) math.newLocalDate(y + 400, m, d) - MS_PER_400_YEARS else math.newLocalDate(y, m, d)

    fun startOf(units: String) = apply {
        val unit = normalizeUnit(units)
        if (!valid) return@apply
        val time = when (unit) {
            "year" -> startOfDate(year(), 0.0, 1.0)
            "quarter" -> startOfDate(year(), month() - month() % 3, 1.0)
            "month" -> startOfDate(year(), month(), 1.0)
            "week" -> startOfDate(year(), month(), date() - weekday())
            "day" -> startOfDate(year(), month(), date())
            "hour" -> d - mod(d + utcOffset() * JsDateMath.MS_PER_MINUTE, JsDateMath.MS_PER_HOUR)
            "minute" -> d - mod(d, JsDateMath.MS_PER_MINUTE)
            "second" -> d - mod(d, JsDateMath.MS_PER_SECOND)
            else -> error(units)
        }
        d = JsDateMath.timeClip(time)
    }

    fun endOf(units: String) = apply {
        val unit = normalizeUnit(units)
        if (!valid) return@apply
        val time = when (unit) {
            "year" -> startOfDate(year() + 1, 0.0, 1.0) - 1
            "quarter" -> startOfDate(year(), month() - month() % 3 + 3, 1.0) - 1
            "month" -> startOfDate(year(), month() + 1, 1.0) - 1
            "week" -> startOfDate(year(), month(), date() - weekday() + 7) - 1
            "day" -> startOfDate(year(), month(), date() + 1) - 1
            "hour" -> d + JsDateMath.MS_PER_HOUR - mod(d + utcOffset() * JsDateMath.MS_PER_MINUTE, JsDateMath.MS_PER_HOUR) - 1
            "minute" -> d + JsDateMath.MS_PER_MINUTE - mod(d, JsDateMath.MS_PER_MINUTE) - 1
            "second" -> d + JsDateMath.MS_PER_SECOND - mod(d, JsDateMath.MS_PER_SECOND) - 1
            else -> error(units)
        }
        d = JsDateMath.timeClip(time)
    }

    // ---- businessAdd of the moment-business plugin bundled with the engine

    fun businessAdd(input: Any?): ScriptDate {
        val sign = if (jsLessThanZero(input)) -1 else 1
        val n = abs(jsNumber(input))
        val result = clone().add(7 * floor(n / 5) * sign, "d")
        var i = n % 5
        if (i.isFinite() && i != floor(i)) {
            throw AutomationFunctionException("businessAdd with a fractional count does not finish")
        }
        while (i != 0.0 && !i.isNaN()) {
            result.add(sign, "d")
            val day = result.day()
            if (day != 0.0 && day != 6.0) i--
        }
        return result
    }

    companion object {
        private const val DOW = 0
        private const val DOY = 6
        private const val MS_PER_400_YEARS = 146_097 * 86_400_000.0
        private val DIGITS = Regex("^\\d+$")
        private val UNIT_ALIASES = mapOf(
            "y" to "year", "year" to "year", "years" to "year",
            "Q" to "quarter", "quarter" to "quarter", "quarters" to "quarter",
            "M" to "month", "month" to "month", "months" to "month",
            "w" to "week", "week" to "week", "weeks" to "week",
            "d" to "day", "day" to "day", "days" to "day", "date" to "day", "dates" to "day", "D" to "day",
            "h" to "hour", "hour" to "hour", "hours" to "hour",
            "m" to "minute", "minute" to "minute", "minutes" to "minute",
            "s" to "second", "second" to "second", "seconds" to "second",
            "ms" to "millisecond", "millisecond" to "millisecond", "milliseconds" to "millisecond",
        )
        private val MONTHS = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")

        fun normalizeUnit(unit: String): String? = UNIT_ALIASES[unit] ?: UNIT_ALIASES[unit.lowercase()]

        fun mod(a: Double, b: Double): Double = ((a % b) + b) % b

        /** Math.round: the closest integer, halves up (exact also for 0.49999999999999994 and values above 2^52) */
        fun jsRound(value: Double): Double {
            if (value.isNaN() || value.isInfinite()) return value
            val down = floor(value)
            val result = if (value - down >= 0.5) down + 1 else down
            return if (result == 0.0 && value < 0) -0.0 else result
        }

        fun absRound(value: Double): Double = if (value < 0) -jsRound(-value) else jsRound(value)

        /** Math.min with NaN */
        fun jsMin(a: Double, b: Double): Double = if (a.isNaN() || b.isNaN()) Double.NaN else minOf(a, b)

        /** moment toInt */
        fun toInt(value: Any?): Double {
            val coerced = jsNumber(value)
            return if (coerced != 0.0 && coerced.isFinite()) (if (coerced < 0) -floor(-coerced) else floor(coerced)) else 0.0
        }

        /** "en" monthsParse (not strict): the month name or its short form at the start */
        fun monthsParse(value: String): Double? {
            for (i in 0 until 12) {
                val regex = Regex("^" + MONTHS[i] + "|^" + MONTHS[i].substring(0, 3), RegexOption.IGNORE_CASE)
                if (regex.containsMatchIn(value)) return i.toDouble()
            }
            return null
        }

        fun isLeapYear(year: Any?): Boolean {
            val y = jsNumber(year)
            return (y % 4 == 0.0 && y % 100 != 0.0) || y % 400 == 0.0
        }

        fun daysInYear(year: Double): Double = if (isLeapYear(year)) 366.0 else 365.0

        /** moment daysInMonth(year, month); year can be a string, then `year += ...` concatenates */
        fun daysInMonth(year: Any?, month: Any?): Double {
            val m = jsNumber(month)
            if (jsNumber(year).isNaN() || m.isNaN()) return Double.NaN
            val modMonth = mod(m, 12.0)
            val delta = (m - modMonth) / 12
            val y: Any? = if (year is String) year + jsString(delta) else jsNumber(year) + delta
            return if (modMonth == 1.0) (if (isLeapYear(y)) 29.0 else 28.0) else 31 - modMonth % 7 % 2
        }

        /** `n < 0` */
        fun jsLessThanZero(value: Any?): Boolean = when (value) {
            null -> false
            else -> jsNumber(value) < 0
        }
    }
}
