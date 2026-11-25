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
import io.lumeer.datafilters.model.SelectOption
import io.lumeer.datafilters.util.formatUnknownDataValue
import io.lumeer.datafilters.util.jsStrictEquals
import io.lumeer.datafilters.util.jsString
import io.lumeer.datafilters.util.jsStringOrEmpty
import io.lumeer.datafilters.util.jsTrim
import io.lumeer.datafilters.util.nonBlankJsValues

class SelectDataValue(val value: Any?, private val config: ConstraintConfig, private val context: FilterContext) : DataValue {

    private val displayValues: Boolean
    val options: List<SelectOption>

    init {
        val selectionListId = config.raw("selectionListId")
        val selectionList = if (jsStringOrEmpty(selectionListId).isNotEmpty()) {
            context.constraintData?.selectionLists?.firstOrNull { it.id == selectionListId }
        } else null
        displayValues = selectionList?.displayValues ?: config.truthy("displayValues")
        val configOptions = selectionList?.options ?: config.selectOptions
        options = nonBlankJsValues(value).map { v ->
            val option = configOptions.firstOrNull { jsString(it.value) == jsString(v) }
            if (option != null) {
                SelectOption(option.value, if (displayValues) option.displayValue else option.value?.let { jsString(it) })
            } else {
                SelectOption(v, jsString(v))
            }
        }
    }

    override fun format(): String {
        if (options.isEmpty()) {
            return formatUnknownDataValue(value)
        }
        return options.joinToString(", ") { option ->
            val shown: Any? = if (displayValues) option.displayValue?.takeIf { it.isNotEmpty() } ?: option.value else option.value
            if (shown == null) "" else jsString(shown)
        }
    }

    override fun meetCondition(condition: ConditionType?, values: List<ConditionValue>): Boolean {
        val otherOptions = values.firstOrNull()?.let { SelectDataValue(it.value, config, context).options }.orEmpty()
        fun SelectOption.isIn(list: List<SelectOption>) = list.any { jsStrictEquals(it.value, value) }
        return when (condition) {
            ConditionType.HasSome, ConditionType.Equals -> options.any { it.isIn(otherOptions) }
            ConditionType.HasNoneOf, ConditionType.NotEquals -> options.none { it.isIn(otherOptions) }
            ConditionType.In -> options.isNotEmpty() && options.all { it.isIn(otherOptions) }
            ConditionType.HasAll -> otherOptions.all { it.isIn(options) }
            ConditionType.IsEmpty -> options.isEmpty() && jsTrim(format()).isEmpty()
            ConditionType.NotEmpty -> options.isNotEmpty() || jsTrim(format()).isNotEmpty()
            else -> false
        }
    }
}
