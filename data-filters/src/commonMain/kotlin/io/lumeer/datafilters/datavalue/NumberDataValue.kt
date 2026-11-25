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
import io.lumeer.datafilters.number.USA
import io.lumeer.datafilters.util.formatUnknownDataValue
import io.lumeer.datafilters.util.isNumeric
import io.lumeer.datafilters.util.jsString
import io.lumeer.datafilters.util.jsStringOrEmpty
import io.lumeer.datafilters.util.jsTrim
import io.lumeer.datafilters.util.jsTruthy
import io.lumeer.datafilters.util.numberToJsString
import io.lumeer.datafilters.util.toNumber

class NumberDataValue(val value: Any?, private val config: ConstraintConfig, private val context: FilterContext) : DataValue {

    private val locale: String = context.constraintData?.locale?.takeIf { it.isNotEmpty() } ?: USA
    private val currency: String? = config.raw("currency")?.takeIf { jsTruthy(it) }?.let { jsString(it) }

    /** Value prepared for parsing: a Double, or a raw value / string that still needs unformatting. */
    private val parsedValue: Any?
    val number: Decimal?
    private val roundedNumber: Decimal?

    init {
        context.numbro.setLanguage(currency ?: locale)
        parsedValue = parseValue(value)
        val unformatted = Numbro.unformat(parsedValue, context.numbro.language)
        number = unformatted?.let { Decimal.parse(numberToJsString(it)) }
        roundedNumber = number.roundedTo(config.int("decimals"))
    }

    private fun parseValue(value: Any?): Any? {
        if (value is Number || isNumeric(value)) {
            return toNumber(value)
        }
        if (config.truthy("separated") || currency != null) {
            return value
        }
        return jsTrim(jsStringOrEmpty(value).replaceFirst(",", "."))
    }

    override fun format(): String {
        val number = number ?: return formatUnknownDataValue(value)
        // like numbro(number.toFixed()): the number goes through the current numbro language and a JS double
        val numbroConfig = numbroConfig()
        val numbro = context.numbro
        if (currency != null) numbro.setLanguage(USA)
        val parsed = Numbro.unformat(number.toPlainString(), numbro.language) ?: Double.NaN
        if (currency == null) {
            return Numbro.formatNumber(parsed, numbroConfig, numbro.language)
        }
        numbro.setLanguage(currency)
        return Numbro.formatCurrency(parsed, numbroConfig, numbro.language)
    }

    private fun numbroConfig(): NumbroFormat {
        val separated = config.truthy("separated")
        val average = config.truthy("compact")
        val mantissa = config.raw("decimals")?.takeIf { isNumeric(it) }?.let { toNumber(it).toInt() }
        return NumbroFormat(
            forceSign = if (config.truthy("forceSign")) true else null,
            thousandSeparated = if (separated) true else null,
            spaceSeparated = if (separated) true else null,
            spaceSeparatedCurrency = if (currency != null) context.numbro.language.spaceSeparatedCurrency else null,
            average = average,
            lowPrecision = if (average) false else null,
            negative = if (config.truthy("negative")) "parenthesis" else null,
            mantissa = mantissa,
            trimMantissa = if (mantissa != null) false else null,
        )
    }

    override fun meetCondition(condition: ConditionType?, values: List<ConditionValue>): Boolean {
        val dataValues = values.map { NumberDataValue(it.value, config, context) }
        return dataValuesMeetConditionByNumber(condition, roundedNumber, dataValues.map { it.roundedNumber }, parsedValue, dataValues.map { it.parsedValue })
    }

}
