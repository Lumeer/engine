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

/** Operation of the "date change" block (the block's OP field values). */
enum class DateChangeOperation(val value: String) {
    Add("add"),
    Subtract("subtract"),
    Set("set");

    companion object {
        fun fromValue(value: String): DateChangeOperation =
            entries.firstOrNull { it.value == value } ?: throw IllegalArgumentException("Unknown date change operation: $value")
    }
}

/**
 * Unit of the "date change" block (the block's UNIT field values). Sticky units keep the last day of a month:
 * a date on the last day of its month ends on the last day of the resulting month.
 */
enum class DateChangeUnit(val value: String, internal val period: String, val sticky: Boolean = false) {
    Seconds("seconds", "seconds"),
    Minutes("minutes", "minutes"),
    Hours("hours", "hours"),
    Days("days", "days"),
    BusinessDays("businessDays", "businessDays"),
    Weeks("weeks", "weeks"),
    Months("months", "months"),
    StickyMonths("sticky_months", "months", true),
    Quarters("quarters", "quarters"),
    StickyQuarters("sticky_quarters", "quarters", true),
    Years("years", "years"),
    StickyYears("sticky_years", "years", true);

    companion object {
        fun fromValue(value: String): DateChangeUnit =
            entries.firstOrNull { it.value == value } ?: throw IllegalArgumentException("Unknown date change unit: $value")
    }
}

/** Operation of the "shift date" block. */
enum class DateShiftOperation(val value: String) {
    StartOf("startOf"),
    EndOf("endOf");

    companion object {
        fun fromValue(value: String): DateShiftOperation =
            entries.firstOrNull { it.value == value } ?: throw IllegalArgumentException("Unknown date shift operation: $value")
    }
}

/** Unit of the "shift date" block. */
enum class DateShiftUnit(val value: String) {
    Minute("minute"),
    Hour("hour"),
    Day("day"),
    Week("week"),
    Month("month"),
    Quarter("quarter"),
    Year("year");

    companion object {
        fun fromValue(value: String): DateShiftUnit =
            entries.firstOrNull { it.value == value } ?: throw IllegalArgumentException("Unknown date shift unit: $value")
    }
}
