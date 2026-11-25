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
import io.lumeer.datafilters.model.ConditionType
import io.lumeer.datafilters.model.ConditionValue
import io.lumeer.datafilters.number.Decimal
import io.lumeer.datafilters.util.formatUnknownDataValue
import io.lumeer.datafilters.util.isNumeric
import io.lumeer.datafilters.util.jsString
import io.lumeer.datafilters.util.jsTrim
import io.lumeer.datafilters.util.numberToJsString
import io.lumeer.datafilters.util.toNumber

class PercentageDataValue(val value: Any?, private val config: ConstraintConfig) : DataValue {

    private val parsedValue: Any? = if (jsTrim(jsString(value)).endsWith("%")) parseInputValue(value) else value
    private val number: Decimal? = convertPercentageToBig(parsedValue)
    private val roundedNumber: Decimal? = number.roundedTo(config.int("decimals"))

    override fun format(): String {
        val big = roundedNumber ?: return formatUnknownDataValue(value)
        return big.toBigJsString() + "%"
    }

    override fun meetCondition(condition: ConditionType?, values: List<ConditionValue>): Boolean {
        val dataValues = values.map { PercentageDataValue(it.value, config) }
        return dataValuesMeetConditionByNumber(condition, roundedNumber, dataValues.map { it.roundedNumber }, parsedValue, dataValues.map { it.parsedValue })
    }


    private companion object {
        /** "50%" -> "0.5"; called only for values ending with % */
        fun parseInputValue(inputValue: Any?): String {
            val prefix = jsTrim(jsString(inputValue)).dropLast(1)
            if (isNumeric(prefix)) {
                return Decimal.parse(numberToJsString(toNumber(prefix)))?.shift(-2)?.toBigJsString() ?: jsString(inputValue)
            }
            return jsString(inputValue)
        }

        fun convertPercentageToBig(value: Any?): Decimal? {
            val big = Decimal.parse(jsString(value)) ?: return null
            return big.shift(2)
        }
    }
}
