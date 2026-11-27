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

import io.lumeer.datafilters.filter.FilterContext
import io.lumeer.datafilters.model.ConditionType
import io.lumeer.datafilters.model.ConditionValue
import io.lumeer.datafilters.model.ConditionValueType
import io.lumeer.datafilters.model.Team
import io.lumeer.datafilters.util.formatUnknownDataValue
import io.lumeer.datafilters.util.jsStrictEquals
import io.lumeer.datafilters.util.jsTrim
import io.lumeer.datafilters.util.nonBlankJsValues

class TeamDataValue(val value: Any?, private val context: FilterContext) : DataValue {

    val teams: List<Team>

    init {
        val allTeams = context.constraintData?.teams.orEmpty()
        teams = nonBlankJsValues(value).mapNotNull { id -> allTeams.firstOrNull { jsStrictEquals(it.id, id) } }
    }

    override fun format(): String =
        if (teams.isNotEmpty()) teams.joinToString(", ") { it.name.orEmpty() } else formatUnknownDataValue(value)

    private fun mapQueryConditionValue(value: ConditionValue): TeamDataValue {
        val currentUser = context.constraintData?.currentUser
        if (value.type == ConditionValueType.CURRENT_TEAMS && currentUser != null) {
            val ids = context.constraintData?.teams.orEmpty().filter { it.users.contains(currentUser.id) }.map { it.id }
            return TeamDataValue(ids, context)
        }
        return TeamDataValue(value.value, context)
    }

    override fun meetCondition(condition: ConditionType?, values: List<ConditionValue>): Boolean {
        val otherTeams = values.firstOrNull()?.let { mapQueryConditionValue(it).teams }.orEmpty()
        fun Team.isIn(list: List<Team>) = list.any { it.id == id }
        return when (condition) {
            ConditionType.HasSome, ConditionType.Equals -> teams.any { it.isIn(otherTeams) }
            ConditionType.HasNoneOf, ConditionType.NotEquals -> teams.none { it.isIn(otherTeams) }
            ConditionType.In -> teams.isNotEmpty() && teams.all { it.isIn(otherTeams) }
            ConditionType.HasAll -> otherTeams.all { it.isIn(teams) }
            ConditionType.IsEmpty -> teams.isEmpty() && jsTrim(format()).isEmpty()
            ConditionType.NotEmpty -> teams.isNotEmpty() || jsTrim(format()).isNotEmpty()
            else -> false
        }
    }
}
