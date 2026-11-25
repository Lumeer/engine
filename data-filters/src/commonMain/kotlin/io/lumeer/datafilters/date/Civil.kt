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

internal const val MS_PER_DAY = 86_400_000L

/** Proleptic Gregorian calendar arithmetic on "wall clock" epoch millis, equivalent to JS Date UTC methods. */
internal object Civil {

    fun floorDiv(a: Long, b: Long): Long {
        val q = a / b
        return if (a % b != 0L && (a xor b) < 0) q - 1 else q
    }

    fun floorMod(a: Long, b: Long): Long = a - floorDiv(a, b) * b

    /** Days since 1970-01-01 for the given year, month (1-12) and day (1-31). */
    fun daysFromCivil(year: Long, month: Int, day: Int): Long {
        val y = if (month <= 2) year - 1 else year
        val era = floorDiv(y, 400)
        val yoe = y - era * 400
        val mp = (month + 9) % 12
        val doy = (153 * mp + 2) / 5 + day - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146097 + doe - 719468
    }

    /** (year, month 1-12, day) */
    fun civilFromDays(days: Long): Triple<Long, Int, Int> {
        val z = days + 719468
        val era = floorDiv(z, 146097)
        val doe = z - era * 146097
        val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val day = (doy - (153 * mp + 2) / 5 + 1).toInt()
        val month = (if (mp < 10) mp + 3 else mp - 9).toInt()
        val year = yoe + era * 400 + (if (month <= 2) 1 else 0)
        return Triple(year, month, day)
    }

    /** JS Date.UTC(year, month0, day, hours, minutes, seconds, ms) including overflow normalization. */
    fun fieldsToMillis(year: Long, month0: Long, day: Long, hours: Long = 0, minutes: Long = 0, seconds: Long = 0, millis: Long = 0): Long {
        val y = year + floorDiv(month0, 12)
        val m = floorMod(month0, 12).toInt() + 1
        val days = daysFromCivil(y, m, 1) + day - 1
        return days * MS_PER_DAY + hours * 3_600_000L + minutes * 60_000L + seconds * 1000L + millis
    }

    fun isLeapYear(year: Long): Boolean = (year % 4 == 0L && year % 100 != 0L) || year % 400 == 0L

    fun daysInMonth(year: Long, month0: Long): Int {
        val modMonth = floorMod(month0, 12)
        val y = year + floorDiv(month0, 12)
        return if (modMonth == 1L) (if (isLeapYear(y)) 29 else 28) else (31 - ((modMonth % 7) % 2)).toInt()
    }

    fun daysInYear(year: Long): Int = if (isLeapYear(year)) 366 else 365
}

/** Broken-down wall clock fields. */
internal class Fields(val wall: Long) {
    val year: Long
    val month0: Int
    val date: Int
    val hours: Int
    val minutes: Int
    val seconds: Int
    val millis: Int
    val dayOfWeek: Int // 0 = Sunday
    val epochDay: Long

    init {
        epochDay = Civil.floorDiv(wall, MS_PER_DAY)
        val msOfDay = Civil.floorMod(wall, MS_PER_DAY)
        val (y, m, d) = Civil.civilFromDays(epochDay)
        year = y
        month0 = m - 1
        date = d
        hours = (msOfDay / 3_600_000L).toInt()
        minutes = ((msOfDay / 60_000L) % 60).toInt()
        seconds = ((msOfDay / 1000L) % 60).toInt()
        millis = (msOfDay % 1000L).toInt()
        dayOfWeek = Civil.floorMod(epochDay + 4, 7).toInt()
    }
}
