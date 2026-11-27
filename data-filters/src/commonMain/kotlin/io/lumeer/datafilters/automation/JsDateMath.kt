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

import io.lumeer.datafilters.date.Civil
import io.lumeer.datafilters.date.TimeZones
import io.lumeer.datafilters.util.jsString
import io.lumeer.datafilters.util.stringToJsNumber
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.truncate

/** JS ToNumber */
internal fun jsNumber(value: Any?): Double = when (value) {
    null -> 0.0
    JsUndefined -> Double.NaN
    is JsDate -> value.time
    is Number -> value.toDouble()
    is Boolean -> if (value) 1.0 else 0.0
    is String -> stringToJsNumber(value)
    is Map<*, *> -> Double.NaN
    else -> stringToJsNumber(jsString(value))
}

/**
 * ECMAScript Date algorithms (local time in [zone]) on time values in milliseconds, NaN for an Invalid Date.
 * The original code mutates JS Date objects with these setters.
 */
internal class JsDateMath(private val zone: String) {

    private fun offset(t: Double): Double = TimeZones.offsetMillis(zone, t.toLong()).toDouble()

    /** LocalTime(t), t must be a valid time value */
    fun localTime(t: Double): Double = t + offset(t)

    /** UTC(t) for a local time */
    fun utc(local: Double): Double {
        if (!local.isFinite()) return Double.NaN
        // far outside the time value range, the offset does not matter (TimeClip gives NaN)
        if (abs(local) > MAX_TIME + 2 * MS_PER_DAY) return local
        return TimeZones.wallToEpochMillis(zone, local.toLong()).toDouble()
    }

    // ---- fields of a local time

    fun day(t: Double): Double = floor(t / MS_PER_DAY)
    fun timeWithinDay(t: Double): Double = mod(t, MS_PER_DAY)
    private fun civil(t: Double) = Civil.civilFromDays(day(t).toLong())
    fun yearFromTime(t: Double): Double = civil(t).first.toDouble()
    fun monthFromTime(t: Double): Double = (civil(t).second - 1).toDouble()
    fun dateFromTime(t: Double): Double = civil(t).third.toDouble()
    fun weekDay(t: Double): Double = mod(day(t) + 4, 7.0)
    fun hourFromTime(t: Double): Double = floor(timeWithinDay(t) / MS_PER_HOUR)
    fun minFromTime(t: Double): Double = mod(floor(t / MS_PER_MINUTE), 60.0)
    fun secFromTime(t: Double): Double = mod(floor(t / MS_PER_SECOND), 60.0)
    fun msFromTime(t: Double): Double = mod(t, MS_PER_SECOND)

    // ---- getters (local)

    private inline fun get(time: Double, field: (Double) -> Double): Double = if (time.isNaN()) Double.NaN else field(localTime(time))
    fun getFullYear(time: Double) = get(time, ::yearFromTime)
    fun getMonth(time: Double) = get(time, ::monthFromTime)
    fun getDate(time: Double) = get(time, ::dateFromTime)
    fun getDay(time: Double) = get(time, ::weekDay)
    fun getHours(time: Double) = get(time, ::hourFromTime)
    fun getMinutes(time: Double) = get(time, ::minFromTime)
    fun getSeconds(time: Double) = get(time, ::secFromTime)

    /** Date.prototype.getTimezoneOffset */
    fun getTimezoneOffset(time: Double): Double = if (time.isNaN()) Double.NaN else (time - localTime(time)) / MS_PER_MINUTE

    // ---- setters (local), arguments are JS values

    fun setFullYear(time: Double, year: Any?, month: Any? = MISSING, date: Any? = MISSING): Double {
        val t = if (time.isNaN()) 0.0 else localTime(time)
        val y = jsNumber(year)
        val m = if (month === MISSING) monthFromTime(t) else jsNumber(month)
        val dt = if (date === MISSING) dateFromTime(t) else jsNumber(date)
        return timeClip(utc(makeDate(makeDay(y, m, dt), timeWithinDay(t))))
    }

    fun setMonth(time: Double, month: Any?, date: Any? = MISSING): Double {
        val m = jsNumber(month)
        val dt = if (date === MISSING) null else jsNumber(date)
        if (time.isNaN()) return Double.NaN
        val t = localTime(time)
        return timeClip(utc(makeDate(makeDay(yearFromTime(t), m, dt ?: dateFromTime(t)), timeWithinDay(t))))
    }

    fun setDate(time: Double, date: Any?): Double {
        val dt = jsNumber(date)
        if (time.isNaN()) return Double.NaN
        val t = localTime(time)
        return timeClip(utc(makeDate(makeDay(yearFromTime(t), monthFromTime(t), dt), timeWithinDay(t))))
    }

    fun setHours(time: Double, hours: Any?): Double {
        val h = jsNumber(hours)
        if (time.isNaN()) return Double.NaN
        val t = localTime(time)
        return timeClip(utc(makeDate(day(t), makeTime(h, minFromTime(t), secFromTime(t), msFromTime(t)))))
    }

    fun setMinutes(time: Double, minutes: Any?): Double {
        val m = jsNumber(minutes)
        if (time.isNaN()) return Double.NaN
        val t = localTime(time)
        return timeClip(utc(makeDate(day(t), makeTime(hourFromTime(t), m, secFromTime(t), msFromTime(t)))))
    }

    fun setSeconds(time: Double, seconds: Any?): Double {
        val s = jsNumber(seconds)
        if (time.isNaN()) return Double.NaN
        val t = localTime(time)
        return timeClip(utc(makeDate(day(t), makeTime(hourFromTime(t), minFromTime(t), s, msFromTime(t)))))
    }

    /** new Date(year, month, date) */
    fun newLocalDate(year: Double, month: Double, date: Double): Double {
        var y = year
        if (!y.isNaN()) {
            val yi = truncate(y)
            if (yi in 0.0..99.0) y = 1900 + yi
        }
        return timeClip(utc(makeDate(makeDay(y, month, date), 0.0)))
    }

    companion object {
        const val MS_PER_SECOND = 1000.0
        const val MS_PER_MINUTE = 60_000.0
        const val MS_PER_HOUR = 3_600_000.0
        const val MS_PER_DAY = 86_400_000.0
        const val MAX_TIME = 8.64e15

        /** an argument that was not passed */
        val MISSING = Any()

        fun mod(a: Double, b: Double): Double {
            val r = a % b
            return if (r < 0) r + b else r
        }

        private fun integer(value: Double): Double = truncate(value) + 0.0

        fun makeDay(year: Double, month: Double, date: Double): Double {
            if (!year.isFinite() || !month.isFinite() || !date.isFinite()) return Double.NaN
            val y = integer(year)
            val m = integer(month)
            val dt = integer(date)
            val ym = y + floor(m / 12)
            if (abs(ym) > 1e9) return Double.NaN
            val mn = mod(m, 12.0)
            return Civil.daysFromCivil(ym.toLong(), mn.toInt() + 1, 1).toDouble() + dt - 1
        }

        fun makeTime(hour: Double, min: Double, sec: Double, ms: Double): Double {
            if (!hour.isFinite() || !min.isFinite() || !sec.isFinite() || !ms.isFinite()) return Double.NaN
            return integer(hour) * MS_PER_HOUR + integer(min) * MS_PER_MINUTE + integer(sec) * MS_PER_SECOND + integer(ms)
        }

        fun makeDate(day: Double, time: Double): Double {
            if (!day.isFinite() || !time.isFinite()) return Double.NaN
            val tv = day * MS_PER_DAY + time
            return if (tv.isFinite()) tv else Double.NaN
        }

        fun timeClip(time: Double): Double {
            if (!time.isFinite() || abs(time) > MAX_TIME) return Double.NaN
            return integer(time)
        }
    }
}
