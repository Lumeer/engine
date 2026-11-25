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
package io.lumeer.datafilters.constraint

import io.lumeer.datafilters.datavalue.ActionDataValue
import io.lumeer.datafilters.datavalue.AddressDataValue
import io.lumeer.datafilters.datavalue.BooleanDataValue
import io.lumeer.datafilters.datavalue.ColorDataValue
import io.lumeer.datafilters.datavalue.CoordinatesDataValue
import io.lumeer.datafilters.datavalue.DataValue
import io.lumeer.datafilters.datavalue.DateTimeDataValue
import io.lumeer.datafilters.datavalue.DurationDataValue
import io.lumeer.datafilters.datavalue.FilesDataValue
import io.lumeer.datafilters.datavalue.LinkDataValue
import io.lumeer.datafilters.datavalue.NumberDataValue
import io.lumeer.datafilters.datavalue.PercentageDataValue
import io.lumeer.datafilters.datavalue.SelectDataValue
import io.lumeer.datafilters.datavalue.TeamDataValue
import io.lumeer.datafilters.datavalue.TextDataValue
import io.lumeer.datafilters.datavalue.UnknownDataValue
import io.lumeer.datafilters.datavalue.UserDataValue
import io.lumeer.datafilters.datavalue.ViewDataValue
import io.lumeer.datafilters.filter.FilterContext
import io.lumeer.datafilters.model.ConstraintType
import io.lumeer.datafilters.model.SelectOption
import io.lumeer.datafilters.util.jsString
import io.lumeer.datafilters.util.jsTruthy
import io.lumeer.datafilters.util.toJsList

interface Constraint {
    val type: ConstraintType

    fun createDataValue(value: Any?, context: FilterContext): DataValue
}

/** Raw constraint config as stored in the DB (values from JSON / Mongo). */
class ConstraintConfig(private val map: Map<String, Any?>) {
    fun raw(key: String): Any? = map[key]
    fun truthy(key: String): Boolean = jsTruthy(map[key])
    fun string(key: String): String? = map[key]?.let { jsString(it) }
    fun int(key: String): Int? = (map[key] as? Number)?.toDouble()?.takeIf { it % 1.0 == 0.0 }?.toInt()

    @Suppress("UNCHECKED_CAST")
    fun map(key: String): Map<String, Any?>? = map[key] as? Map<String, Any?>

    fun list(key: String): List<Any?> = toJsList(map[key]).orEmpty()

    /** Select options of a Select constraint, parsed once */
    internal val selectOptions: List<SelectOption> by lazy { parseSelectOptions(list("options")) }

    companion object {
        val EMPTY = ConstraintConfig(emptyMap())
    }
}

object UnknownConstraint : Constraint {
    override val type = ConstraintType.Unknown
    override fun createDataValue(value: Any?, context: FilterContext): DataValue = UnknownDataValue(value)
}

enum class CaseStyle { None, LowerCase, UpperCase, TitleCase, SentenceCase }

private class SimpleConstraint(override val type: ConstraintType, private val factory: (Any?, FilterContext) -> DataValue) : Constraint {
    override fun createDataValue(value: Any?, context: FilterContext): DataValue = factory(value, context)
}

/** Select options from a config list of maps. */
internal fun parseSelectOptions(list: List<Any?>): List<SelectOption> = list.mapNotNull { item ->
    @Suppress("UNCHECKED_CAST")
    val option = item as? Map<String, Any?> ?: return@mapNotNull null
    SelectOption(option["value"], option["displayValue"]?.let { jsString(it) })
}

/**
 * Creates a constraint from its serialized form (type name + config map, as stored in the DB),
 * like createConstraint in the original library.
 */
fun createConstraint(type: String?, config: Map<String, Any?>?): Constraint {
    val cfg = config?.let { ConstraintConfig(it) } ?: ConstraintConfig.EMPTY
    val constraintType = ConstraintType.fromValue(type)
    val factory: (Any?, FilterContext) -> DataValue = when (constraintType) {
        ConstraintType.Text -> textFactory(cfg)
        ConstraintType.Number -> { value, ctx -> NumberDataValue(value, cfg, ctx) }
        ConstraintType.Percentage -> { value, _ -> PercentageDataValue(value, cfg) }
        ConstraintType.Duration -> { value, ctx -> DurationDataValue(value, cfg, ctx) }
        ConstraintType.DateTime -> { value, ctx -> DateTimeDataValue(value, cfg, ctx) }
        ConstraintType.Boolean -> { value, _ -> BooleanDataValue(value) }
        ConstraintType.Select -> { value, ctx -> SelectDataValue(value, cfg, ctx) }
        ConstraintType.User -> { value, ctx -> UserDataValue(value, ctx) }
        ConstraintType.Team -> { value, ctx -> TeamDataValue(value, ctx) }
        ConstraintType.View -> { value, _ -> ViewDataValue(value) }
        ConstraintType.Link -> { value, _ -> LinkDataValue(value) }
        ConstraintType.Files -> { value, _ -> FilesDataValue(value) }
        ConstraintType.Action -> { _, _ -> ActionDataValue(cfg.raw("title")) }
        ConstraintType.Color -> { value, _ -> ColorDataValue(value) }
        ConstraintType.Coordinates -> { value, _ -> CoordinatesDataValue(value, cfg) }
        ConstraintType.Address -> addressFactory(cfg)
        else -> return UnknownConstraint
    }
    return SimpleConstraint(constraintType, factory)
}

private fun textFactory(config: ConstraintConfig): (Any?, FilterContext) -> DataValue {
    val caseStyle = config.string("caseStyle")?.let { style -> CaseStyle.entries.firstOrNull { it.name == style } }
    return { value, _ -> TextDataValue(value, caseStyle) }
}

private fun addressFactory(config: ConstraintConfig): (Any?, FilterContext) -> DataValue {
    val fields = config.list("fields").map { jsString(it) }
    return { value, _ -> AddressDataValue(value, fields) }
}
