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

import kotlin.math.floor

/**
 * How "local" moments behave, mirroring moment + moment-timezone in the original library:
 * - ZONE: a default zone was set (moment.tz.setDefault). Setters keep the wall clock and then re-read the zone offset.
 * - NATIVE: no default zone, moments behave like JS Date in the system zone.
 */
internal enum class LocalMode { ZONE, NATIVE }

/** Time settings for one filter run. */
internal class TimeContext(timezone: String?, now: Long?, systemZone: String = TimeZones.systemZone()) {

    /**
     * The original bundle contains a second moment copy (inside @lumeer/utils) used by parseMomentDate.
     * moment-timezone is attached only to the first one, so dates parsed by format always use the system zone.
     */
    val utilsContext: TimeContext by lazy { if (timezone == null) this else TimeContext(null, now, systemZone) }

    val mode: LocalMode
    val zone: String
    val nowMillis: Long = now ?: TimeZones.currentTimeMillis()
    val systemZone: String = systemZone

    init {
        if (timezone != null && timezone.isNotEmpty() && TimeZones.isValidZone(timezone)) {
            mode = LocalMode.ZONE
            zone = timezone
        } else {
            mode = LocalMode.NATIVE
            zone = systemZone
        }
    }
}

/**
 * Minimal port of the parts of moment.js that the filters use.
 * Stores the wall clock (as epoch millis of the same fields in UTC) and the offset in minutes east of UTC.
 */
internal class DateInstant private constructor(
    val ctx: TimeContext,
    val isUtc: Boolean,
    val isValid: Boolean,
    private var wall: Long,
    private var offset: Long,
) {

    val valueOf: Long get() = wall - offset

    /** moment utcOffset() in minutes */
    val utcOffset: Int get() = (offset / 60_000L).toInt()

    /** moment 2.31 utcOffset(): -Math.round(Date#getTimezoneOffset()) */
    val utcOffsetRounded: Int get() = if (isUtc) 0 else floor(offset / 60_000.0 + 0.5).toInt()

    private var cachedFields: Fields? = null

    /** Wall clock fields, cached until the next setter */
    private val fields: Fields
        get() = cachedFields?.takeIf { it.wall == wall } ?: Fields(wall).also { cachedFields = it }

    fun clone(): DateInstant = DateInstant(ctx, isUtc, isValid, wall, offset)

    // ---- getters

    fun year(): Long = fields.year
    fun month(): Int = fields.month0
    fun date(): Int = fields.date
    fun day(): Int = fields.dayOfWeek
    fun hours(): Int = fields.hours
    fun minutes(): Int = fields.minutes
    fun seconds(): Int = fields.seconds
    fun milliseconds(): Int = fields.millis

    fun dayOfYear(): Int {
        val f = fields
        return (f.epochDay - Civil.daysFromCivil(f.year, 1, 1)).toInt() + 1
    }

    fun quarter(): Int = (month() + 1 + 2) / 3

    /** Locale weekday (moment's global locale "fr": Monday = 0) */
    fun weekday(): Int = (day() + 7 - LOCALE_DOW) % 7

    /** "en" weekday used when formatting */
    fun weekdayEn(): Int = (day() + 7 - EN_DOW) % 7

    fun isoWeekday(): Int = if (day() == 0) 7 else day()

    /** Locale week (moment's global locale "fr", ISO-like weeks) */
    fun week(): Int = weekOfYear(this, LOCALE_DOW, LOCALE_DOY).first

    /** "en" week and week year used when formatting */
    fun weekEn(): Int = weekOfYear(this, EN_DOW, EN_DOY).first
    fun weekYearEn(): Long = weekOfYear(this, EN_DOW, EN_DOY).second
    fun isoWeek(): Int = weekOfYear(this, 1, 4).first
    fun isoWeekYear(): Long = weekOfYear(this, 1, 4).second

    // ---- setters (moment mutates and returns this)

    private fun setWall(newWall: Long): DateInstant {
        if (!isValid) return this
        when {
            isUtc -> {
                wall = newWall
                offset = 0
            }
            ctx.mode == LocalMode.ZONE -> {
                // moment-timezone updateOffset(mom, keepTime = true)
                val o2 = TimeZones.offsetMillis(ctx.zone, newWall - offset)
                val instant = newWall - o2
                val o3 = TimeZones.offsetMillis(ctx.zone, instant)
                offset = o3
                wall = instant + o3
            }
            else -> {
                // JS Date local setters
                val instant = TimeZones.wallToEpochMillis(ctx.zone, newWall)
                offset = TimeZones.offsetMillis(ctx.zone, instant)
                wall = instant + offset
            }
        }
        return this
    }

    private fun withFields(year: Long, month0: Long, date: Long, hours: Long, minutes: Long, seconds: Long, millis: Long): DateInstant =
        setWall(Civil.fieldsToMillis(year, month0, date, hours, minutes, seconds, millis))

    fun setYear(value: Long): DateInstant {
        val f = fields
        val date = if (Civil.isLeapYear(f.year) && f.month0 == 1 && f.date == 29) Civil.daysInMonth(value, 1).toLong() else f.date.toLong()
        return withFields(value, f.month0.toLong(), date, f.hours.toLong(), f.minutes.toLong(), f.seconds.toLong(), f.millis.toLong())
    }

    fun setMonth(value: Long): DateInstant {
        val f = fields
        val dayOfMonth = minOf(f.date, Civil.daysInMonth(f.year, value)).toLong()
        return withFields(f.year, value, dayOfMonth, f.hours.toLong(), f.minutes.toLong(), f.seconds.toLong(), f.millis.toLong())
    }

    fun setDate(value: Long): DateInstant {
        val f = fields
        return withFields(f.year, f.month0.toLong(), value, f.hours.toLong(), f.minutes.toLong(), f.seconds.toLong(), f.millis.toLong())
    }

    fun setHours(value: Long): DateInstant {
        val f = fields
        return withFields(f.year, f.month0.toLong(), f.date.toLong(), value, f.minutes.toLong(), f.seconds.toLong(), f.millis.toLong())
    }

    fun setMinutes(value: Long): DateInstant {
        val f = fields
        return withFields(f.year, f.month0.toLong(), f.date.toLong(), f.hours.toLong(), value, f.seconds.toLong(), f.millis.toLong())
    }

    fun setSeconds(value: Long): DateInstant {
        val f = fields
        return withFields(f.year, f.month0.toLong(), f.date.toLong(), f.hours.toLong(), f.minutes.toLong(), value, f.millis.toLong())
    }

    fun setMilliseconds(value: Long): DateInstant {
        val f = fields
        return withFields(f.year, f.month0.toLong(), f.date.toLong(), f.hours.toLong(), f.minutes.toLong(), f.seconds.toLong(), value)
    }

    fun addDays(days: Long): DateInstant = if (days == 0L) this else setDate(date() + days)

    fun addMonths(months: Long): DateInstant = if (months == 0L) this else setMonth(month() + months)

    fun setDay(value: Long): DateInstant = addDays(value - day())

    fun setWeek(value: Long): DateInstant = addDays((value - week()) * 7)

    fun startOf(unit: String): DateInstant {
        if (!isValid) return this
        val f = fields
        return when (unit) {
            "year" -> withFields(f.year, 0, 1, 0, 0, 0, 0)
            "quarter" -> withFields(f.year, (f.month0 - f.month0 % 3).toLong(), 1, 0, 0, 0, 0)
            "month" -> withFields(f.year, f.month0.toLong(), 1, 0, 0, 0, 0)
            "week" -> withFields(f.year, f.month0.toLong(), (f.date - weekday()).toLong(), 0, 0, 0, 0)
            "day" -> withFields(f.year, f.month0.toLong(), f.date.toLong(), 0, 0, 0, 0)
            else -> this
        }
    }

    // ---- comparison (moment returns false / NaN for invalid moments)

    fun isSame(other: DateInstant?): Boolean = other != null && isValid && other.isValid && valueOf == other.valueOf
    fun isAfter(other: DateInstant?): Boolean = other != null && isValid && other.isValid && valueOf > other.valueOf
    fun isBefore(other: DateInstant?): Boolean = other != null && isValid && other.isValid && valueOf < other.valueOf
    fun isSameOrAfter(other: DateInstant?): Boolean = isSame(other) || isAfter(other)
    fun isSameOrBefore(other: DateInstant?): Boolean = isSame(other) || isBefore(other)

    /** a.diff(b) in milliseconds, NaN when invalid */
    fun diff(other: DateInstant): Double = if (isValid && other.isValid) (valueOf - other.valueOf).toDouble() else Double.NaN

    fun format(format: String): String = if (isValid) DateFormatter.format(this, format) else "Invalid date"

    // moment-timezone: zone abbreviation only for moments in the default zone
    fun zoneAbbr(): String = when {
        isUtc -> "UTC"
        ctx.mode == LocalMode.ZONE -> TimeZones.zoneAbbreviation(ctx.zone, valueOf)
        else -> ""
    }

    companion object {
        const val EN_DOW = 0
        const val EN_DOY = 6

        /** The original bundle imports moment locales and the last one ("fr") stays as moment's global locale. */
        const val LOCALE_DOW = 1
        const val LOCALE_DOY = 4

        fun invalid(ctx: TimeContext, utc: Boolean): DateInstant = DateInstant(ctx, utc, false, 0, 0L)

        /** moment() / moment.utc() */
        fun now(ctx: TimeContext, utc: Boolean): DateInstant = fromInstant(ctx, utc, ctx.nowMillis)

        fun fromInstant(ctx: TimeContext, utc: Boolean, epochMillis: Long): DateInstant {
            val offset = if (utc) 0L else TimeZones.offsetMillis(ctx.zone, epochMillis)
            return DateInstant(ctx, utc, true, epochMillis + offset, offset)
        }

        /** DateInstant from local wall clock fields (no explicit offset in the input). */
        fun fromWall(ctx: TimeContext, utc: Boolean, wall: Long): DateInstant {
            if (utc) return DateInstant(ctx, true, true, wall, 0L)
            val instant = TimeZones.wallToEpochMillis(ctx.zone, wall)
            return fromInstant(ctx, false, instant)
        }

        // week-calendar-utils.js
        private fun firstWeekOffset(year: Long, dow: Int, doy: Int): Int {
            val fwd = 7 + dow - doy
            val fwdDay = Civil.floorMod(Civil.daysFromCivil(year, 1, 1) + fwd - 1 + 4, 7).toInt()
            val fwdlw = (7 + fwdDay - dow) % 7
            return -fwdlw + fwd - 1
        }

        fun weeksInYear(year: Long, dow: Int, doy: Int): Int {
            val weekOffset = firstWeekOffset(year, dow, doy)
            val weekOffsetNext = firstWeekOffset(year + 1, dow, doy)
            return (Civil.daysInYear(year) - weekOffset + weekOffsetNext) / 7
        }

        /** (week, weekYear) */
        fun weekOfYear(mom: DateInstant, dow: Int, doy: Int): Pair<Int, Long> {
            val year = mom.year()
            val weekOffset = firstWeekOffset(year, dow, doy)
            val week = floor((mom.dayOfYear() - weekOffset - 1) / 7.0).toInt() + 1
            return when {
                week < 1 -> Pair(week + weeksInYear(year - 1, dow, doy), year - 1)
                week > weeksInYear(year, dow, doy) -> Pair(week - weeksInYear(year, dow, doy), year + 1)
                else -> Pair(week, year)
            }
        }

        /** (year, dayOfYear) */
        fun dayOfYearFromWeeks(year: Long, week: Int, weekday: Int, dow: Int, doy: Int): Pair<Long, Int> {
            val localWeekday = (7 + weekday - dow) % 7
            val weekOffset = firstWeekOffset(year, dow, doy)
            val dayOfYear = 1 + 7 * (week - 1) + localWeekday + weekOffset
            return when {
                dayOfYear <= 0 -> Pair(year - 1, Civil.daysInYear(year - 1) + dayOfYear)
                dayOfYear > Civil.daysInYear(year) -> Pair(year + 1, dayOfYear - Civil.daysInYear(year))
                else -> Pair(year, dayOfYear)
            }
        }

    }
}
