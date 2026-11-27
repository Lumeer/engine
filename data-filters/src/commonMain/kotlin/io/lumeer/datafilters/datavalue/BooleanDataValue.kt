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
import io.lumeer.datafilters.util.jsTruthy
import io.lumeer.datafilters.util.toJsList

private val TRUTHY_VALUES = setOf("true", "yes", "ja", "ano", "áno", "sí", "si", "sim", "да", "是", "はい", "vâng", "כן")

class BooleanDataValue(value: Any?) : DataValue {

    /** an array is true when all its items are truthy */
    val booleanValue: Boolean = when (val single = toJsList(value)?.all { jsTruthy(it) } ?: value) {
        true -> true
        is String -> single.lowercase() in TRUTHY_VALUES
        else -> false
    }

    override fun format(): String = booleanValue.toString()

    override fun meetCondition(condition: ConditionType?, values: List<ConditionValue>): Boolean {
        val other = values.isNotEmpty() && BooleanDataValue(values[0].value).booleanValue
        return when (condition) {
            ConditionType.Equals -> booleanValue == other
            ConditionType.NotEquals -> booleanValue != other
            else -> false
        }
    }
}
