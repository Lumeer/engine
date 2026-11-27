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
import io.lumeer.datafilters.filter.FilterContext
import io.lumeer.datafilters.model.ConditionType
import io.lumeer.datafilters.model.ConditionValue
import io.lumeer.datafilters.number.Decimal
import io.lumeer.datafilters.number.Numbro
import io.lumeer.datafilters.number.NumbroFormat
import io.lumeer.datafilters.util.formatUnknownDataValue
import io.lumeer.datafilters.util.isNumeric
import io.lumeer.datafilters.util.jsString
import io.lumeer.datafilters.util.jsToNumber
import io.lumeer.datafilters.util.jsStringOrEmpty
import io.lumeer.datafilters.util.jsTruthy
import io.lumeer.datafilters.util.numberToJsString
import io.lumeer.datafilters.util.removeJsWhitespace
import io.lumeer.datafilters.util.toNumber

private val SORTED_UNITS = listOf("w", "d", "h", "m", "s")

/** \d*(unit1|unit2|...) for parsing "1w2d" like values */
private fun unitsRegex(units: Set<String>): Regex = Regex("\\d*(" + units.joinToString("|") { Regex.escape(it) } + ")")

class DurationDataValue(val value: Any?, private val config: ConstraintConfig, private val context: FilterContext) : DataValue {

    private val unitsMap: Map<String, String>? = context.constraintData?.durationUnitsMap
    private val decimalPlaces: Int = config.int("decimalPlaces") ?: 0

    /** Value in milliseconds (or the original text when it is not a valid duration), null when invalid */
    private val saved: Any? = if (isValid(value, unitsMap, true)) saveValue(value) else null
    private val number: Decimal? = saved?.let { Decimal.parse(jsString(it)) }
    private val roundedNumber: Decimal? = number.roundedTo(config.int("decimalPlaces"))

    override fun format(): String {
        val save = saved
        if (number == null || save == null) return formatUnknownDataValue(value)
        if (!isNumeric(save)) return jsString(save)
        val result = countsToString(unitsCountsMap(save), decimalPlaces)
        return result.ifEmpty { if (toNumber(save) >= 0) "0" else "" }
    }

    override fun meetCondition(condition: ConditionType?, values: List<ConditionValue>): Boolean {
        val dataValues = values.map { DurationDataValue(it.value, config, context) }
        return dataValuesMeetConditionByNumber(condition, roundedNumber, dataValues.map { it.roundedNumber }, value, dataValues.map { it.value })
    }

    // ---- duration.utils.ts

    private fun prepare(value: Any?): String = removeJsWhitespace(jsStringOrEmpty(value).lowercase())

    private fun validByLetters(value: Any?, letters: Collection<String>): Boolean {
        val stringValue = prepare(value)
        if (stringValue.isEmpty()) return false
        val allowed = letters.joinToString("")
        return stringValue.all { it in '0'..'9' || allowed.indexOf(it) >= 0 }
    }

    private fun isValid(value: Any?, unitsMap: Map<String, String>?, allowNegative: Boolean): Boolean =
        (isNumeric(value) && (allowNegative || toNumber(value) >= 0)) ||
            validByLetters(value, SORTED_UNITS) ||
            validByLetters(value, unitsMap?.values.orEmpty())

    private fun saveValue(value: Any?): Any {
        if (isValid(value, unitsMap, false)) {
            return when {
                isNumeric(value) -> Decimal.parse(jsString(value))?.toFixed(0) ?: toNumber(value)
                validByLetters(value, unitsMap?.values.orEmpty()) -> parseToMillis(value, unitToMillisMap(unitsMap))
                validByLetters(value, SORTED_UNITS) -> parseToMillis(value, unitToMillisMap(null))
                else -> 0
            }
        }
        return jsStringOrEmpty(value)
    }

    private fun defaultConversion(type: String, unit: String): Double = when (unit) {
        "w" -> if (type == "Work") 5.0 else 7.0
        "d" -> if (type == "Work") 8.0 else 24.0
        "h", "m" -> 60.0
        else -> 1000.0
    }

    private fun unitToMillis(unit: String, type: String): Double {
        val conversion = config.map("conversions")?.get(unit)?.takeIf { jsTruthy(it) }?.let { jsToNumber(it) } ?: defaultConversion(type, unit)
        return if (unit == "s") conversion else conversion * unitToMillis(SORTED_UNITS[SORTED_UNITS.indexOf(unit) + 1], type)
    }

    private fun unitToMillisMap(unitsMap: Map<String, String>?): Map<String, Double> {
        val type = config.raw("type")?.takeIf { jsTruthy(it) }?.let { jsString(it) } ?: "Work"
        val map = linkedMapOf<String, Double>()
        for (unit in SORTED_UNITS) {
            val key = unitsMap?.get(unit)?.takeIf { it.isNotEmpty() } ?: unit
            map[key] = unitToMillis(unit, type)
        }
        return map
    }

    private fun parseToMillis(value: Any?, unitToMillis: Map<String, Double>): String {
        val regex = unitsRegex(unitToMillis.keys)
        var millis = Decimal.ZERO
        for (group in regex.findAll(prepare(value)).map { it.value }) {
            val perGroup = Decimal.parse(numberToJsString(unitToMillis[group.last().toString()] ?: return "NaN")) ?: return "NaN"
            val multiplier = Decimal.parse(group.filter { it in '0'..'9' }) ?: Decimal.of(1)
            millis += perGroup * multiplier
        }
        return millis.toFixed(0)
    }

    private fun unitsCountsMap(save: Any): Map<String, Double> {
        val multiplier = if (toNumber(save) >= 0) 1 else -1
        val toMillis = unitToMillisMap(null)
        var current = (Decimal.parse(jsString(save)) ?: Decimal.ZERO) * Decimal.of(multiplier.toLong())
        var usedNumUnits = 0
        val maximumUnits = config.int("maxUnits")?.takeIf { it != 0 } ?: Int.MAX_VALUE
        var units = SORTED_UNITS
        config.raw("maxUnit")?.takeIf { jsTruthy(it) }?.let { maxUnit ->
            val index = SORTED_UNITS.indexOf(jsString(maxUnit))
            units = if (index >= 0) SORTED_UNITS.subList(index, SORTED_UNITS.size) else SORTED_UNITS.takeLast(1)
        }
        val result = linkedMapOf<String, Double>()
        val one = Decimal.of(1)
        for (unit in units) {
            val millis = toMillis[unit] ?: continue
            if (millis == 0.0 || millis.isNaN()) continue
            if (usedNumUnits >= maximumUnits) continue
            val isLastUnit = usedNumUnits + 1 == maximumUnits || unit == "s"
            val unitBig = Decimal.parse(numberToJsString(millis)) ?: continue
            val numUnits = current.div(unitBig) ?: continue
            var rounded = numUnits.roundDown(decimalPlaces)
            if (numUnits < one) {
                result[unit] = (if (isLastUnit && numUnits.signum > 0) 1.0 else 0.0) * multiplier
                continue
            }
            current -= rounded * unitBig
            val half = unitBig.div(Decimal.of(2))
            if (usedNumUnits + 1 == maximumUnits && half != null && current > half) {
                rounded += one
            }
            result[unit] = toNumber(rounded.toFixed(decimalPlaces)) * multiplier
            usedNumUnits++
        }
        return result
    }

    private fun countsToString(counts: Map<String, Double>, decimalPlaces: Int): String {
        var minusSignAdded = false
        val result = StringBuilder()
        for (unit in SORTED_UNITS) {
            val numUnits = Decimal.parse(numberToJsString(counts[unit] ?: 0.0)) ?: Decimal.ZERO
            if (numUnits.signum == 0) continue
            val unitString = unitsMap?.get(unit)?.takeIf { it.isNotEmpty() } ?: unit
            val shown = if (numUnits.signum < 0 && minusSignAdded) numUnits.negate() else numUnits
            if (numUnits.signum < 0) minusSignAdded = true
            result.append(formatUnits(shown, decimalPlaces)).append(unitString)
        }
        return result.toString()
    }

    private fun formatUnits(numUnits: Decimal, decimalPlaces: Int): String {
        if (decimalPlaces == 0) return numUnits.toFixed(0)
        val language = context.numbro.language
        val parsed = Numbro.unformat(numUnits.toFixed(decimalPlaces), language) ?: Double.NaN
        return Numbro.formatNumber(parsed, NumbroFormat(trimMantissa = true, mantissa = decimalPlaces), language)
    }
}
