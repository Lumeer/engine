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
package io.lumeer.datafilters.model

enum class ConditionType(val value: String) {
    Equals("eq"),
    NotEquals("neq"),
    LowerThan("lt"),
    LowerThanEquals("lte"),
    GreaterThan("gt"),
    GreaterThanEquals("gte"),
    In("in"),
    HasSome("hasSome"),
    HasAll("hasAll"),
    HasNoneOf("nin"),
    Between("between"),
    NotBetween("notBetween"),
    Contains("contains"),
    NotContains("notContains"),
    StartsWith("startsWith"),
    EndsWith("endsWith"),
    IsEmpty("empty"),
    NotEmpty("notEmpty"),
    Enabled("enabled"),
    Disabled("disabled");

    companion object {
        fun fromValue(value: String?): ConditionType? = entries.firstOrNull { it.value == value }
    }
}

enum class ConstraintType(val value: String) {
    Unknown("None"),
    Text("Text"),
    Number("Number"),
    Address("Address"),
    Boolean("Boolean"),
    Action("Action"),
    Coordinates("Coordinates"),
    DateTime("DateTime"),
    Duration("Duration"),
    Email("Email"),
    Files("FileAttachment"),
    Image("Image"),
    Link("Link"),
    Percentage("Percentage"),
    Rating("Rating"),
    Select("Select"),
    Tag("Tag"),
    User("User"),
    Team("Team"),
    View("View"),
    Color("Color");

    companion object {
        fun fromValue(value: String?): ConstraintType = entries.firstOrNull { it.value == value } ?: Unknown
    }
}

/** Raw value of a filter condition, as stored in the query. */
class ConditionValue(val type: String? = null, val value: Any? = null)

open class AttributeFilter(
    val attributeId: String,
    val condition: ConditionType?,
    val conditionValues: List<ConditionValue> = emptyList(),
)

class CollectionAttributeFilter(
    val collectionId: String,
    attributeId: String,
    condition: ConditionType?,
    conditionValues: List<ConditionValue> = emptyList(),
) : AttributeFilter(attributeId, condition, conditionValues)

class LinkAttributeFilter(
    val linkTypeId: String,
    attributeId: String,
    condition: ConditionType?,
    conditionValues: List<ConditionValue> = emptyList(),
) : AttributeFilter(attributeId, condition, conditionValues)

class QueryStem(
    val collectionId: String,
    val linkTypeIds: List<String> = emptyList(),
    val documentIds: Set<String> = emptySet(),
    val filters: List<CollectionAttributeFilter> = emptyList(),
    val linkFilters: List<LinkAttributeFilter> = emptyList(),
)

class Query(
    val stems: List<QueryStem> = emptyList(),
    val fulltexts: List<String> = emptyList(),
    val page: Int? = null,
    val pageSize: Int? = null,
)

/** Special values of [ConditionValue.type] for user and team conditions */
object ConditionValueType {
    const val CURRENT_USER = "currentUser"
    const val CURRENT_TEAMS = "currentTeams"
}

class User(val id: String?, val name: String?, val email: String?)

class Team(val id: String?, val name: String?, val users: List<String> = emptyList())

class SelectOption(val value: Any?, val displayValue: String? = null)

class SelectionList(val id: String?, val displayValues: Boolean = false, val options: List<SelectOption> = emptyList())

class CurrencyData(val abbreviations: List<String> = emptyList(), val ordinals: List<String> = emptyList())

/**
 * External data needed to interpret values, the same as ConstraintData in the original library.
 * [nowMillis] fixes "now" for relative date conditions (tests), [locale] is a language tag such as "en-US".
 */
class ConstraintData(
    val users: List<User> = emptyList(),
    val teams: List<Team> = emptyList(),
    val currentUser: User? = null,
    val durationUnitsMap: Map<String, String>? = null,
    val currencyData: CurrencyData? = null,
    val selectionLists: List<SelectionList> = emptyList(),
    val timezone: String? = null,
    val locale: String? = null,
    val nowMillis: Long? = null,
)

enum class EquationOperator(val value: String) {
    Or("or"),
    And("and");

    companion object {
        fun fromValue(value: String?): EquationOperator? = entries.firstOrNull { it.value == value }
    }
}

class AttributeFilterEquation(
    val equations: List<AttributeFilterEquation> = emptyList(),
    val filter: AttributeFilter? = null,
    val operator: EquationOperator? = null,
)

/** [AttributeLockExceptionGroup.type] values */
object AttributeLockGroupType {
    const val EVERYONE = "everyone"
    const val USERS_AND_TEAMS = "usersAndTeams"
}

class AttributeLockExceptionGroup(
    val type: String? = null,
    val typeValue: List<String> = emptyList(),
    val equation: AttributeFilterEquation? = null,
)

class AttributeLock(
    val locked: Boolean = false,
    val exceptionGroups: List<AttributeLockExceptionGroup> = emptyList(),
)
