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
import io.lumeer.datafilters.util.jsStrictEquals
import io.lumeer.datafilters.util.nonBlankJsValues

/** The backend has no views in the constraint data, so views are known only by id and are formatted as "". */
class ViewDataValue(val value: Any?) : DataValue {

    val viewIds: List<Any?> = nonBlankJsValues(value)

    override fun format(): String = ""

    override fun meetCondition(condition: ConditionType?, values: List<ConditionValue>): Boolean {
        val other = values.firstOrNull()?.let { ViewDataValue(it.value).viewIds }.orEmpty()
        fun Any?.isIn(list: List<Any?>) = list.any { jsStrictEquals(it, this) }
        return when (condition) {
            ConditionType.HasSome, ConditionType.Equals -> viewIds.any { it.isIn(other) }
            ConditionType.HasNoneOf, ConditionType.NotEquals -> viewIds.none { it.isIn(other) }
            ConditionType.In -> viewIds.isNotEmpty() && viewIds.all { it.isIn(other) }
            ConditionType.HasAll -> other.all { it.isIn(viewIds) }
            ConditionType.IsEmpty -> viewIds.isEmpty()
            ConditionType.NotEmpty -> viewIds.isNotEmpty()
            else -> false
        }
    }
}
