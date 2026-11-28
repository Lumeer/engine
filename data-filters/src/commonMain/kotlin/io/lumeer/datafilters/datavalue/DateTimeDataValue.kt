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
package io.lumeer.datafilters.datavalue

import io.lumeer.datafilters.constraint.ConstraintConfig
import io.lumeer.datafilters.date.DateInstant
import io.lumeer.datafilters.date.DateParser
import io.lumeer.datafilters.date.TimeContext
import io.lumeer.datafilters.filter.FilterContext
import io.lumeer.datafilters.model.ConditionType
import io.lumeer.datafilters.model.ConditionValue
import io.lumeer.datafilters.util.formatUnknownDataValue
import io.lumeer.datafilters.util.jsStrictEquals
import io.lumeer.datafilters.util.jsString
import io.lumeer.datafilters.util.jsTrim
import io.lumeer.datafilters.util.jsTruthy
import io.lumeer.datafilters.util.valueMeetFulltexts

/** Dates are formatted with the moment "en" locale, the backend never sets another one. */
class DateTimeDataValue(val value: Any?, private val config: ConstraintConfig, private val context: FilterContext) : DataValue {

    private val format: String? = config.raw("format")?.takeIf { jsTruthy(it) }?.let { jsString(it) }
    private val isUtc: Boolean = config.truthy("asUtc")
    private val time: TimeContext = context.time
    internal val momentDate: DateInstant?

    init {
        var parsed: DateInstant? = null
        if (jsTruthy(value)) {
            parsed = if (isIsoFormat(value)) {
                DateParser.parse(jsString(value), isUtc, time)
            } else {
                parseMomentDate(value, format, isUtc, time.utilsContext)
            }
        }
        momentDate = if (parsed != null && parsed.isValid) resetUnusedMomentPart(parsed, format) else parsed
    }

    private fun isValidMomentDate(): Boolean = momentDate != null && momentDate.isValid

    override fun format(): String {
        if (value == null || value == "") return ""
        if (!isValidMomentDate()) return formatUnknownDataValue(value)
        return if (format != null) momentDate!!.format(format) else formatUnknownDataValue(value)
    }

    override fun meetCondition(condition: ConditionType?, values: List<ConditionValue>): Boolean {
        val otherMomentValues = mapConditionValues(values)
        val momentDates = sortMoments(otherMomentValues.map { resetUnusedMomentPart(momentDate, it.second) }) { it }

        val otherMoment = otherMomentValues.getOrNull(0)?.first
        if (momentDate == null && otherMoment == null) {
            if (condition == ConditionType.Equals) {
                val otherValue = values.getOrNull(0)?.value
                return (!jsTruthy(value) && !jsTruthy(otherValue)) || jsStrictEquals(value, otherValue)
            }
        } else if (momentDate == null || otherMoment == null) {
            if (condition == ConditionType.NotEquals) {
                return true
            }
        }

        val allDefined = (0 until conditionTypeNumberOfInputs(condition)).all { index ->
            momentDates.getOrNull(index) != null && otherMomentValues.getOrNull(index)?.first != null
        }
        if (!allDefined) {
            return false
        }

        return when (condition) {
            ConditionType.Equals -> momentDates[0]!!.isSame(otherMoment)
            ConditionType.NotEquals -> !momentDates[0]!!.isSame(otherMoment)
            ConditionType.GreaterThan -> momentDates[0]!!.isAfter(otherMoment)
            ConditionType.GreaterThanEquals -> momentDates[0]!!.isSameOrAfter(otherMoment)
            ConditionType.LowerThan -> momentDates[0]!!.isBefore(otherMoment)
            ConditionType.LowerThanEquals -> momentDates[0]!!.isSameOrBefore(otherMoment)
            ConditionType.Between -> momentDates[0]!!.isSameOrAfter(otherMoment) && momentDates[1]!!.isSameOrBefore(otherMomentValues[1].first)
            ConditionType.NotBetween -> momentDates[0]!!.isBefore(otherMoment) || momentDates[1]!!.isAfter(otherMomentValues[1].first)
            ConditionType.IsEmpty -> value == null || jsTrim(jsString(value)).isEmpty()
            ConditionType.NotEmpty -> value != null && jsTrim(jsString(value)).isNotEmpty()
            else -> false
        }
    }

    private fun mapConditionValues(values: List<ConditionValue>): List<Pair<DateInstant?, String?>> {
        val mapped = values.map { value ->
            val type = value.type
            if (!type.isNullOrEmpty()) {
                Pair(conditionValueMoment(type), conditionValueFormat(type))
            } else {
                Pair(resetUnusedMomentPart(DateTimeDataValue(value.value, config, context).momentDate, format), format)
            }
        }
        return sortMoments(mapped) { it.first }
    }

    override fun meetFullTexts(fulltexts: List<String>): Boolean = valueMeetFulltexts(format(), fulltexts)

    private fun conditionValueMoment(type: String): DateInstant? {
        val base = DateInstant.now(time, isUtc)
        return when (type) {
            "yesterday" -> base.startOf("day").addDays(-1)
            "tomorrow" -> base.startOf("day").addDays(1)
            "today" -> base.startOf("day")
            "lastWeek" -> resetWeek(base.startOf("day").addDays(-7))
            "nextWeek" -> resetWeek(base.startOf("day").addDays(7))
            "thisWeek" -> resetWeek(base.startOf("day"))
            "lastMonth" -> base.startOf("month").addMonths(-1)
            "nextMonth" -> base.startOf("month").addMonths(1)
            "thisMonth" -> base.startOf("month")
            else -> null
        }
    }

    private fun conditionValueFormat(type: String): String = when (type) {
        "yesterday", "tomorrow", "today" -> "DD M Y"
        "lastWeek", "nextWeek", "thisWeek" -> "W Y"
        "lastMonth", "nextMonth", "thisMonth" -> "M Y"
        else -> ""
    }

    companion object {
        private val ISO_FORMAT = Regex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[^\\n\\r\\u2028\\u2029]\\d{3}")
        private val DATE_FORMATS = listOf("DD.MM.YYYY", "YYYY-MM-DD", "DD/MM/YYYY", "MM/DD/YYYY", "YYYY", "DD.MM.")
        private val DAY_FORMAT = Regex("^(d|dd)$", RegexOption.IGNORE_CASE)

        private fun isIsoFormat(value: Any?): Boolean = ISO_FORMAT.containsMatchIn(if (jsTruthy(value)) jsString(value) else "")

        /** JS Array.prototype.sort with the compareMoments comparator (NaN counts as 0); lists have at most two items */
        private fun <T> sortMoments(items: List<T>, moment: (T) -> DateInstant?): List<T> {
            val result = items.toMutableList()
            for (i in 1 until result.size) {
                var j = i
                while (j > 0 && compareMoments(moment(result[j - 1]), moment(result[j])) > 0) {
                    val tmp = result[j]
                    result[j] = result[j - 1]
                    result[j - 1] = tmp
                    j--
                }
            }
            return result
        }

        private fun compareMoments(a: DateInstant?, b: DateInstant?): Double {
            if (a == null || b == null) return if (a != null) 1.0 else if (b != null) -1.0 else 0.0
            val diff = a.diff(b)
            return if (diff.isNaN()) 0.0 else diff
        }

        /** @lumeer/utils parseMomentDate */
        internal fun parseMomentDate(value: Any?, expectedFormat: String?, utc: Boolean, time: TimeContext): DateInstant? {
            if (!jsTruthy(value)) return null
            val formats = mutableListOf(DateParser.ISO_8601)
            formats.addAll(DATE_FORMATS)
            if (expectedFormat != null) {
                if (DAY_FORMAT.matches(expectedFormat.lowercase())) {
                    val modifiedValue = if (expectedFormat.length == 1) "0" else jsString(value).take(expectedFormat.length) + ".01.1970"
                    return DateParser.parse(modifiedValue, listOf("DD.MM.YYYY"), utc, time)
                }
                val result = DateParser.parse(value, listOf(expectedFormat), utc, time)
                if (result.isValid) {
                    return result
                }
                formats.add(1, expectedFormat)
            }
            return DateParser.parse(value, formats, utc, time)
        }

        private class DateTimeOptions(format: String) {
            private val lower = format.lowercase()
            val year = lower.contains('y')
            val quarter = lower.contains('q')
            val month = format.contains('M')
            val week = lower.contains('w')
            val day = lower.contains('d')
            val dayOfWeek = lower.contains("ddd")
            val hours = lower.contains('h') || format.contains('k')
            val minutes = format.contains('m')
            val seconds = format.contains('s')
            val milliseconds = format.contains('S')
        }

        /** @lumeer/utils resetUnusedMomentPart (each step works on a clone) */
        internal fun resetUnusedMomentPart(date: DateInstant?, format: String?): DateInstant? {
            if (date == null || format.isNullOrEmpty()) return date
            val options = DateTimeOptions(format)
            var copy: DateInstant = date
            if (!options.year) copy = resetYear(copy, options.dayOfWeek)
            if (!options.month && !options.week && !options.quarter && !options.dayOfWeek) copy = copy.clone().setMonth(0)
            if (!options.day && !options.week && !options.dayOfWeek) copy = copy.clone().setDate(1)
            if (options.dayOfWeek && !options.month) copy = copy.clone().setWeek(2)
            if (options.quarter && !options.month && !options.day) copy = copy.clone().startOf("quarter")
            if (options.week) copy = resetWeek(date)
            if (!options.hours) copy = copy.clone().setHours(0)
            if (!options.minutes) copy = copy.clone().setMinutes(0)
            if (!options.seconds) copy = copy.clone().setSeconds(0)
            if (!options.milliseconds) copy = copy.clone().setMilliseconds(0)
            return copy
        }

        private fun resetYear(date: DateInstant, keepDayOfWeek: Boolean): DateInstant {
            if (keepDayOfWeek) {
                val diffYears = date.year() - 1970
                val cloned = date.clone().addDays(-diffYears * 52 * 7)
                return when (cloned.year()) {
                    1971L -> cloned.addDays(-52 * 7)
                    1969L -> cloned.addDays(52 * 7)
                    else -> cloned
                }
            }
            return date.clone().setYear(1970)
        }

        /** @lumeer/utils resetWeek */
        internal fun resetWeek(date: DateInstant): DateInstant = if (date.week() == 1) date.clone().setDay(5) else date.clone().setDay(1)
    }
}
