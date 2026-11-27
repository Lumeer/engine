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
package io.lumeer.core.js

import io.lumeer.datafilters.automation.AutomationDates
import io.lumeer.datafilters.automation.AutomationHtml
import io.lumeer.datafilters.automation.AutomationNumbers
import io.lumeer.datafilters.automation.DateChangeOperation
import io.lumeer.datafilters.automation.DateChangeUnit
import io.lumeer.datafilters.automation.DateShiftOperation
import io.lumeer.datafilters.automation.DateShiftUnit
import org.graalvm.polyglot.Value

/**
 * Functions the automation blocks call through the lumeer bridge, replacing moment.js in the scripts.
 * The scripts pass operations and units as the block field values (strings).
 */
object AutomationFunctions {

    /** in the server zone, like the script engine */
    private val dates = AutomationDates(now = { System.currentTimeMillis() }, jsDateParse = AutomationJsValues::jsDateParse)

    /** Block "date change": the time value of the resulting date, wrapped by the script in new Date(...) */
    @JvmStatic
    fun changeDate(date: Value?, operation: String, count: Value?, unit: String): Double =
        dates.changeDate(AutomationJsValues.toJs(date), DateChangeOperation.fromValue(operation), AutomationJsValues.toJs(count), DateChangeUnit.fromValue(unit))

    /** for the Java code (converters, e-mails): no script context, ScriptDateParser parses strings like new Date(string) */
    private val javaDates = AutomationDates(now = { System.currentTimeMillis() }, jsDateParse = ScriptDateParser::parse)

    /** formatMomentJsDate / formatMomentJsUtcDate for the Java code, null on an error */
    @JvmStatic
    fun formatDateForJava(time: Long, format: String?, locale: String?, utc: Boolean): String? = try {
        javaDates.formatDate(time.toDouble(), format, locale, utc)
    } catch (e: Exception) {
        null
    }

    /** parseMomentJsDate / parseMomentJsUtcDate for the Java code, null when invalid or on an error */
    @JvmStatic
    fun parseDateForJava(date: String?, format: String?, locale: String?, utc: Boolean): Long? = try {
        javaDates.parseDate(date, format, locale, utc).takeUnless { it.isNaN() }?.toLong()
    } catch (e: Exception) {
        null
    }

    /** Block "format date": formatMomentJsDate(time, format, locale) */
    @JvmStatic
    fun formatDate(time: Value?, format: Value?, locale: Value?): String =
        dates.formatDate(AutomationJsValues.toJs(time), AutomationJsValues.toJs(format), AutomationJsValues.toJs(locale))

    /** Block "parse date": parseMomentJsDate(date, format, locale), the time value (NaN when invalid) */
    @JvmStatic
    fun parseDate(date: Value?, format: Value?, locale: Value?): Double =
        dates.parseDate(AutomationJsValues.toJs(date), AutomationJsValues.toJs(format), AutomationJsValues.toJs(locale))

    /** Block "format currency" */
    @JvmStatic
    fun formatCurrency(locale: String, decimals: Double, value: Value?): String =
        AutomationNumbers.formatCurrency(locale, decimals, AutomationJsValues.toJs(value))

    /** Code generated before for moment: moment(date) with the operations, see [LegacyScriptShim] */
    @JvmStatic
    fun dateChain(date: Value?, operations: Value?): Double {
        val ops = (AutomationJsValues.toJs(operations) as? List<*>).orEmpty().map { (it as? List<*>).orEmpty() }
        return dates.dateChain(AutomationJsValues.toJs(date), ops)
    }

    /** Block "escape HTML" */
    @JvmStatic
    fun escapeHtml(value: Value?): String = AutomationHtml.escape(AutomationJsValues.toJs(value))

    /** Block "unescape HTML" */
    @JvmStatic
    fun unescapeHtml(value: Value?): String = AutomationHtml.unescape(AutomationJsValues.toJs(value))

    /** Block "shift date" */
    @JvmStatic
    fun shiftDate(date: Value?, operation: String, unit: String): Double =
        dates.shiftDate(AutomationJsValues.toJs(date), DateShiftOperation.fromValue(operation), DateShiftUnit.fromValue(unit))
}
