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

import io.lumeer.datafilters.date.DateInstant
import io.lumeer.datafilters.date.DateFormatter
import io.lumeer.datafilters.date.DateLocale
import io.lumeer.datafilters.date.DateParser
import io.lumeer.datafilters.date.TimeContext
import io.lumeer.datafilters.date.TimeZones
import io.lumeer.datafilters.util.jsTruthy


/**
 * Date functions of the automation (Blockly) blocks, replacing moment (2.31) and the moment-business plugin bundled with the engine.
 * Each function returns what the original generated code returned, as the time value of the resulting JS Date
 * (NaN for an Invalid Date). Inputs are JS values: null, [JsUndefined], numbers, strings, booleans, [JsDate].
 *
 * @param zone local time zone (the server zone by default, as the script engine uses it)
 * @param jsDateParse `new Date(string).getTime()` of the script engine, used where moment falls back to it
 *
 * Format and parse date support en, cs, hu, sk, pl, de, de-at, de-ch, es, fr, it, pt, pt-br, nl, ru, uk and en-gb (moment 2.31
 * locale data); "cz" is taken as cs; other locales use their base language when supported, otherwise en.
 */
class AutomationDates(
    zone: String? = null,
    private val now: () -> Long = { TimeZones.currentTimeMillis() },
    private val jsDateParse: ((String) -> Double)? = null,
) {
    private val zone: String = zone ?: TimeZones.systemZone()
    private val factory = ScriptDateFactory(this.zone, now, jsDateParse)

    /** Block "date change". */
    fun changeDate(date: Any?, operation: DateChangeOperation, count: Any?, unit: DateChangeUnit): Double {
        val period = unit.period
        val mom = factory.create(date)
        return when (operation) {
            DateChangeOperation.Add, DateChangeOperation.Subtract -> when {
                // a date on the last day of its month ends on the last day of the resulting month
                // (the block generated add() for subtract too before; rules saved then keep that, see LegacyScriptShim)
                unit.sticky -> {
                    val lastDay = mom.daysInMonth() == mom.date()
                    val changed = addOrSubtract(mom, operation, count, period)
                    if (lastDay) changed.endOf(period) else changed
                }
                // moment(date).businessAdd((count)) or businessAdd(-(count))
                unit == DateChangeUnit.BusinessDays -> mom.businessAdd(if (operation == DateChangeOperation.Add) count else -jsNumber(count))
                else -> addOrSubtract(mom, operation, count, period)
            }
            DateChangeOperation.Set -> when (unit) {
                // moment(date).date((count) - 1)
                DateChangeUnit.Days, DateChangeUnit.BusinessDays -> mom.setDate(jsNumber(count) - 1)
                else -> {
                    val setter = period.removeSuffix("s")
                    if (unit.sticky && mom.daysInMonth() == mom.date()) set(mom, setter, count).endOf(period) else set(mom, setter, count)
                }
            }
        }.valueOf()
    }

    /** Block "shift date": moment(date).startOf(unit) or endOf(unit). */
    fun shiftDate(date: Any?, operation: DateShiftOperation, unit: DateShiftUnit): Double {
        val mom = factory.create(date)
        return when (operation) {
            DateShiftOperation.StartOf -> mom.startOf(unit.value)
            DateShiftOperation.EndOf -> mom.endOf(unit.value)
        }.valueOf()
    }

    /**
     * Block "format date": moment(time).locale(locale).format((format || '').replace(/'/g, "\\'")),
     * moment.utc(time) when [utc] (as the engine converts attribute values).
     */
    fun formatDate(time: Any?, format: Any?, locale: Any?, utc: Boolean = false): String {
        val mom = if (utc) createUtc(time) else factory.create(time)
        val localeKey = localeKey(locale, forFormat = true)
        val pattern = quotedFormat(format).ifEmpty { if (utc) DEFAULT_FORMAT_UTC else DEFAULT_FORMAT }
        val locale = DateLocale.supported(localeKey)
        if (!mom.isValid()) return locale.invalidDate
        val moment = DateInstant.fromInstant(context(), utc, mom.valueOf().toLong())
        return DateFormatter.format(moment, pattern, locale, latest = true)
    }

    /**
     * Block "parse date": moment((date || '').replace(/'/g, "\\'"), (format || '').replace(/'/g, "\\'"), locale).valueOf(),
     * moment.utc(...) when [utc]. NaN for an invalid date.
     */
    fun parseDate(date: Any?, format: Any?, locale: Any?, utc: Boolean = false): Double {
        val input = quotedFormat(date)
        val pattern = quotedFormat(format)
        if (locale is Boolean) throw AutomationFunctionException("Strict parsing is not supported")
        val localeKey = localeKey(locale, forFormat = false)
        val ctx = context()
        val mom = if (pattern.isEmpty()) {
            // no format: moment(string) parsing, which does not use the locale
            if (input.isEmpty()) return jsDateParse?.invoke(input) ?: Double.NaN
            DateParser.parse(input, utc, ctx, jsDateParse, latest = true)
        } else {
            DateParser.parse(input, listOf(pattern), utc, ctx, DateLocale.supported(localeKey), latest = true)
        }
        return if (mom.isValid) mom.valueOf.toDouble() else Double.NaN
    }

    private fun context() = TimeContext(null, now(), zone)

    private fun createUtc(time: Any?): ScriptDate = factory.create(time)

    /** JS truthiness, undefined included */
    private fun truthy(value: Any?): Boolean = value !== JsUndefined && (value is JsDate || value is JsObject || jsTruthy(value))

    /** (value || '').replace(/'/g, "\\'") */
    private fun quotedFormat(value: Any?): String = when {
        !truthy(value) -> ""
        value is String -> value.replace("'", "\\'")
        else -> throw AutomationFunctionException("(...).replace is not a function")
    }

    /** the key moment getLocale gets: falsy keys give the global locale, other non-strings fail in normalizeLocale */
    private fun localeKey(locale: Any?, forFormat: Boolean): String = when {
        // moment().locale() without an argument returns the locale name and .format is not a function of it
        forFormat && locale === JsUndefined -> throw AutomationFunctionException("moment().locale(...).format is not a function")
        !truthy(locale) -> ""
        locale is String -> locale
        else -> throw AutomationFunctionException("key.toLowerCase is not a function")
    }

    /**
     * Code generated by the blocks before (moment calls): moment(date) and then [operations] (name and JS arguments, in order),
     * the time value of the result. A getter (date(), daysInMonth()...) as the last operation gives its number instead.
     */
    fun dateChain(date: Any?, operations: List<List<Any?>>): Double {
        var mom = factory.create(date)
        for ((index, operation) in operations.withIndex()) {
            val name = operation[0] as String
            val args = operation.drop(1)
            val value = args.getOrElse(0) { JsUndefined }
            val getter = index == operations.lastIndex && name in GETTERS && (value == null || value === JsUndefined)
            if (getter) {
                return when (name) {
                    "daysInMonth" -> mom.daysInMonth()
                    "date" -> mom.date()
                    "month" -> mom.month()
                    "year" -> mom.year()
                    else -> throw AutomationFunctionException("Unsupported moment getter $name")
                }
            }
            mom = when (name) {
                "add" -> mom.add(value, args.getOrNull(1) as? String ?: throw AutomationFunctionException("add without a unit"))
                "subtract" -> mom.subtract(value, args.getOrNull(1) as? String ?: throw AutomationFunctionException("subtract without a unit"))
                "startOf" -> mom.startOf(value as? String ?: throw AutomationFunctionException("startOf without a unit"))
                "endOf" -> mom.endOf(value as? String ?: throw AutomationFunctionException("endOf without a unit"))
                "businessAdd" -> mom.businessAdd(value)
                "date" -> mom.setDate(value)
                "second" -> mom.setSecond(value)
                "minute" -> mom.setMinute(value)
                "hour" -> mom.setHour(value)
                "week" -> mom.setWeek(value)
                "month" -> mom.setMonth(value)
                "quarter" -> mom.setQuarter(value)
                "year" -> mom.setYear(value)
                else -> throw AutomationFunctionException("Unsupported moment function $name")
            }
        }
        return mom.valueOf()
    }

    private fun addOrSubtract(mom: ScriptDate, operation: DateChangeOperation, count: Any?, unit: String): ScriptDate =
        if (operation == DateChangeOperation.Add) mom.add(count, unit) else mom.subtract(count, unit)

    companion object {
        private val GETTERS = setOf("daysInMonth", "date", "month", "year")

        /** The moment locale name that format/parse date use for a locale key (unsupported locales: the base language or en) */
        fun effectiveLocale(key: String): String = DateLocale.supported(key).abbr

        /** Locales supported by format/parse date, other keys use their base language or en */
        val SUPPORTED_LOCALES: Set<String> get() = DateLocale.SUPPORTED

        private const val DEFAULT_FORMAT = "YYYY-MM-DDTHH:mm:ssZ"
        private const val DEFAULT_FORMAT_UTC = "YYYY-MM-DDTHH:mm:ss[Z]"
    }

    private fun set(mom: ScriptDate, setter: String, value: Any?): ScriptDate = when (setter) {
        "second" -> mom.setSecond(value)
        "minute" -> mom.setMinute(value)
        "hour" -> mom.setHour(value)
        "week" -> mom.setWeek(value)
        "month" -> mom.setMonth(value)
        "quarter" -> mom.setQuarter(value)
        "year" -> mom.setYear(value)
        else -> throw IllegalArgumentException("Unknown unit $setter")
    }
}
