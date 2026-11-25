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
import io.lumeer.datafilters.number.Decimal
import io.lumeer.datafilters.util.jsStrictEquals
import io.lumeer.datafilters.util.jsString
import io.lumeer.datafilters.util.jsTrim
import io.lumeer.datafilters.util.jsTruthy

/**
 * Text conditions on comparable strings: [comparable] turns this value and each condition value
 * into the string used for comparison.
 */
internal fun <T : DataValue> T.meetTextCondition(condition: ConditionType?, values: List<ConditionValue>, create: (Any?) -> T, comparable: (T) -> String): Boolean =
    dataValuesMeetConditionByText(condition, comparable(this), values.map { comparable(create(it.value)) })

internal fun dataValuesMeetConditionByText(condition: ConditionType?, value: String, otherValues: List<String>): Boolean {
    val other = otherValues.getOrNull(0)
    // JS coerces a missing value to "undefined" in includes/startsWith/endsWith
    val otherText = other ?: "undefined"
    return when (condition) {
        ConditionType.Equals -> other != null && value == other
        ConditionType.NotEquals -> other == null || value != other
        ConditionType.Contains -> value.contains(otherText)
        ConditionType.NotContains -> !value.contains(otherText)
        ConditionType.StartsWith -> value.startsWith(otherText)
        ConditionType.EndsWith -> value.endsWith(otherText)
        ConditionType.IsEmpty -> value.isEmpty()
        ConditionType.NotEmpty -> value.isNotEmpty()
        else -> false
    }
}

internal fun conditionTypeNumberOfInputs(condition: ConditionType?): Int = when (condition) {
    ConditionType.IsEmpty, ConditionType.NotEmpty, ConditionType.Enabled, ConditionType.Disabled -> 0
    ConditionType.Between, ConditionType.NotBetween -> 2
    else -> 1
}

/** `value` is null or only whitespace */
internal fun isBlankJsValue(value: Any?): Boolean = value == null || jsTrim(jsString(value)).isEmpty()

/** Equality of two values that could not be parsed: both empty or strictly equal */
internal fun unparsedValuesEqual(value: Any?, otherValue: Any?): Boolean =
    (!jsTruthy(value) && !jsTruthy(otherValue)) || jsStrictEquals(value, otherValue)

internal fun dataValuesMeetConditionByNumber(
    condition: ConditionType?,
    number: Decimal?,
    otherNumbers: List<Decimal?>,
    value: Any?,
    otherValues: List<Any?>,
): Boolean {
    val other = otherNumbers.getOrNull(0)
    val other2 = otherNumbers.getOrNull(1)
    if (number == null && other == null) {
        if (condition == ConditionType.Equals) {
            return unparsedValuesEqual(value, otherValues.getOrNull(0))
        }
    } else if (number == null || other == null) {
        if (condition == ConditionType.NotEquals) {
            return true
        }
    }

    if (condition == ConditionType.IsEmpty) return isBlankJsValue(value)
    if (condition == ConditionType.NotEmpty) return !isBlankJsValue(value)
    if (number == null || other == null) return false

    return when (condition) {
        ConditionType.Equals -> number.compareTo(other) == 0
        ConditionType.NotEquals -> number.compareTo(other) != 0
        ConditionType.GreaterThan -> number > other
        ConditionType.GreaterThanEquals -> number >= other
        ConditionType.LowerThan -> number < other
        ConditionType.LowerThanEquals -> number <= other
        ConditionType.Between -> other2 != null && number >= other && number <= other2
        ConditionType.NotBetween -> other2 != null && (number < other || number > other2)
        else -> false
    }
}

/** Rounds half up to [decimalPlaces] when it is a non-negative number (roundBigNumber) */
internal fun Decimal?.roundedTo(decimalPlaces: Int?): Decimal? =
    if (this != null && decimalPlaces != null && decimalPlaces >= 0) roundHalfUp(decimalPlaces) else this
