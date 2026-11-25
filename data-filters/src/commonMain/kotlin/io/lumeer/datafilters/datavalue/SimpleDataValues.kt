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

import io.lumeer.datafilters.model.ConditionType
import io.lumeer.datafilters.model.ConditionValue
import io.lumeer.datafilters.util.formatUnknownDataValue
import io.lumeer.datafilters.util.jsString
import io.lumeer.datafilters.util.jsStringOrEmpty
import io.lumeer.datafilters.util.jsTrim
import io.lumeer.datafilters.util.jsTruthy
import io.lumeer.datafilters.util.valueMeetFulltexts

class FilesDataValue(val value: Any?) : DataValue {

    override fun format(): String = formatUnknownDataValue(value)

    override fun meetCondition(condition: ConditionType?, values: List<ConditionValue>): Boolean = when (condition) {
        ConditionType.IsEmpty -> !jsTruthy(value)
        ConditionType.NotEmpty -> jsTruthy(value)
        else -> false
    }
}

/** Action button. Its enabled/disabled conditions are evaluated by the filter from the attribute lock. */
class ActionDataValue(private val title: Any?) : DataValue {

    override fun format(): String = ""

    override fun meetCondition(condition: ConditionType?, values: List<ConditionValue>): Boolean = true

    override fun meetFullTexts(fulltexts: List<String>): Boolean = valueMeetFulltexts(jsStringOrEmpty(title), fulltexts)
}

class AddressDataValue(val value: Any?, private val fields: List<String>) : DataValue {

    override fun format(): String {
        @Suppress("UNCHECKED_CAST")
        val address = value as? Map<String, Any?> ?: return jsStringOrEmpty(value)
        val nonEmptyFields = fields.filter { jsTruthy(address[it]) }
        val result = StringBuilder()
        nonEmptyFields.forEachIndexed { index, field ->
            val fieldValue = jsString(address[field])
            when {
                index == 0 -> result.append(fieldValue)
                field in STREET_FIELDS && nonEmptyFields[index - 1] in STREET_FIELDS -> result.append(' ').append(fieldValue)
                else -> result.append(", ").append(fieldValue)
            }
        }
        return result.toString()
    }

    override fun meetCondition(condition: ConditionType?, values: List<ConditionValue>): Boolean =
        meetTextCondition(condition, values, { AddressDataValue(it, fields) }) { jsTrim(it.format().lowercase()) }

    private companion object {
        val STREET_FIELDS = setOf("houseNumber", "street")
    }
}
