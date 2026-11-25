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
import io.lumeer.datafilters.model.User
import io.lumeer.datafilters.util.formatUnknownDataValue
import io.lumeer.datafilters.util.jsString
import io.lumeer.datafilters.util.jsTrim
import io.lumeer.datafilters.util.nonBlankJsValues

private const val TEAM_PREFIX = "@"

/** Users (by email) and teams (by "@teamId") */
class UserDataValue(val value: Any?, private val context: FilterContext) : DataValue {

    val users: List<User>
    val teams: List<Team>
    val usersIds: List<String?>
    val teamsIds: List<String?>
    val teamsUsersIds: List<String>
    val allUsersIds: List<String?>

    init {
        val allUsers = context.constraintData?.users.orEmpty()
        val allTeams = context.constraintData?.teams.orEmpty()
        val users = mutableListOf<User>()
        val teams = mutableListOf<Team>()
        for (v in nonBlankJsValues(value)) {
            val stringValue = jsString(v)
            if (stringValue.startsWith(TEAM_PREFIX)) {
                val teamId = stringValue.substring(TEAM_PREFIX.length)
                allTeams.firstOrNull { it.id == teamId }?.let { teams.add(it) }
            } else {
                // stored values keep unknown users too (showInvalid)
                val user = allUsers.firstOrNull { it.email == stringValue } ?: User(null, stringValue, stringValue)
                users.add(User(user.id?.takeIf { it.isNotEmpty() } ?: user.email, user.name, user.email))
            }
        }
        this.users = users
        this.teams = teams
        usersIds = users.map { it.id }
        teamsIds = teams.map { it.id }
        teamsUsersIds = teams.flatMap { it.users }.distinct()
        allUsersIds = (usersIds + teamsUsersIds).distinct()
    }

    fun format(preferEmail: Boolean): String {
        if (users.isEmpty() && teams.isEmpty()) {
            return formatUnknownDataValue(value)
        }
        val names = teams.map { it.name.orEmpty() } + users.map { user ->
            val preferred = if (preferEmail) user.email else user.name
            val fallback = if (preferEmail) user.name else user.email
            (preferred?.takeIf { it.isNotEmpty() } ?: fallback).orEmpty()
        }
        return names.joinToString(", ")
    }

    override fun format(): String = format(false)

    private fun mapQueryConditionValue(value: ConditionValue): UserDataValue {
        val currentUser = context.constraintData?.currentUser
        if (value.type == ConditionValueType.CURRENT_USER) {
            return UserDataValue(currentUser?.email, context)
        }
        if (value.type == ConditionValueType.CURRENT_TEAMS && currentUser != null) {
            val teamsIds = context.constraintData?.teams.orEmpty().filter { it.users.contains(currentUser.id) }.map { TEAM_PREFIX + it.id }
            return UserDataValue(teamsIds, context)
        }
        return UserDataValue(value.value, context)
    }

    override fun meetCondition(condition: ConditionType?, values: List<ConditionValue>): Boolean {
        val other = values.firstOrNull()?.let { mapQueryConditionValue(it) }
        val otherUsersIds = other?.usersIds.orEmpty()
        val otherTeamsIds = other?.teamsIds.orEmpty()
        val otherTeamsUsersIds = other?.teamsUsersIds.orEmpty()
        val sharesAny = otherTeamsIds.any { it in teamsIds } || otherTeamsUsersIds.any { it in usersIds } || otherUsersIds.any { it in allUsersIds }

        return when (condition) {
            ConditionType.HasSome, ConditionType.Equals -> sharesAny
            ConditionType.HasNoneOf, ConditionType.NotEquals -> !sharesAny
            ConditionType.In -> (usersIds.isNotEmpty() || teamsIds.isNotEmpty()) && teamsIds.all { it in otherTeamsIds } && usersIds.all { it in otherUsersIds }
            ConditionType.HasAll -> otherTeamsIds.all { it in teamsIds } && otherUsersIds.all { it in allUsersIds }
            ConditionType.IsEmpty -> users.isEmpty() && teams.isEmpty() && jsTrim(format()).isEmpty()
            ConditionType.NotEmpty -> users.isNotEmpty() || teams.isNotEmpty() || jsTrim(format()).isNotEmpty()
            else -> false
        }
    }

    override fun meetFullTexts(fulltexts: List<String>): Boolean =
        super.meetFullTexts(fulltexts) || io.lumeer.datafilters.util.valueMeetFulltexts(format(true), fulltexts)
}
