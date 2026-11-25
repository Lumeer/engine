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
import io.lumeer.datafilters.util.formatUnknownDataValue
import io.lumeer.datafilters.util.isNumeric
import io.lumeer.datafilters.util.jsString
import io.lumeer.datafilters.util.jsToFixed
import io.lumeer.datafilters.util.jsTrim
import kotlin.math.abs
import kotlin.math.floor

class CoordinatesDataValue(val value: Any?, private val config: ConstraintConfig) : DataValue {

    private val coordinates: Pair<Double, Double>? = parseCoordinates(value)

    override fun format(): String {
        val coords = coordinates ?: return formatUnknownDataValue(value)
        val precision = (config.raw("precision") as? Number)?.toDouble()?.takeIf { it in 0.0..20.0 }?.toInt() ?: 0
        val (lat, lng) = coords
        return if (config.raw("format") == "DMS") {
            val longitude = formatNumberCoordinate(lng, precision) + (if (lng > 0) "E" else "W")
            val latitude = formatNumberCoordinate(lat, precision) + (if (lat > 0) "N" else "S")
            "$latitude, $longitude"
        } else {
            jsToFixed(lat, precision) + ", " + jsToFixed(lng, precision)
        }
    }

    private fun formatNumberCoordinate(coordinate: Double, precision: Int): String {
        var value = abs(coordinate)
        val degrees = floor(value)
        value -= degrees
        value *= 60
        val minutes = floor(value)
        val seconds = (value - minutes) * 60
        return jsToFixed(degrees, 0) + "° " + jsToFixed(minutes, 0) + "' " + jsToFixed(seconds, precision) + "\" "
    }

    override fun meetCondition(condition: ConditionType?, values: List<ConditionValue>): Boolean =
        meetTextCondition(condition, values, { CoordinatesDataValue(it, config) }) { jsTrim(it.format()).lowercase() }

    private companion object {
        private val NUMBERS = Regex("-?\\d+(\\.\\d+)?")
        private val INVALID_LETTERS = Regex("(?![neswd])[a-z]", RegexOption.IGNORE_CASE)
        private val ORIENTATION = Regex("^[^nsew]*[ns]?[^nsew]*[ew]?[^nsew]*$", RegexOption.IGNORE_CASE)
        private val SOUTH = Regex("s", RegexOption.IGNORE_CASE)
        private val WEST = Regex("w", RegexOption.IGNORE_CASE)

        /** coordinate-parser; (lat, lng) or null */
        fun parseCoordinates(value: Any?): Pair<Double, Double>? {
            @Suppress("UNCHECKED_CAST")
            (value as? Map<String, Any?>)?.let { map ->
                val lat = map["lat"]
                val lng = map["lng"]
                if (lat != null && lng != null) {
                    return Pair((lat as? Number)?.toDouble() ?: Double.NaN, (lng as? Number)?.toDouble() ?: Double.NaN)
                }
            }
            val input: String = when {
                value == null -> return null
                isNumeric(value) && !jsString(value).contains(",") -> jsString(value) + ",0"
                value is String -> value
                else -> return null
            }
            if (INVALID_LETTERS.containsMatchIn(input) || !ORIENTATION.matches(input)) return null
            val numbers = NUMBERS.findAll(input).map { it.value }.toList()
            if (numbers.isEmpty() || numbers.size % 2 != 0 || numbers.size > 6) return null
            val half = numbers.size / 2
            var latitude = toDecimal(numbers.subList(0, half))
            var longitude = toDecimal(numbers.subList(numbers.size - half, numbers.size))
            if (SOUTH.containsMatchIn(input)) latitude *= -1
            if (WEST.containsMatchIn(input)) longitude *= -1
            return Pair(latitude, longitude)
        }

        private fun toDecimal(numbers: List<String>): Double {
            val normalized = DoubleArray(4)
            numbers.forEachIndexed { i, n -> normalized[i] = n.toDouble() }
            val sign = if (normalized[0] >= 0) 1 else -1
            var degrees = abs(normalized[0])
            var minutes = abs(normalized[1])
            var seconds = abs(normalized[2])
            var milliseconds = abs(normalized[3])
            if (minutes == 0.0 && seconds == 0.0) {
                if (degrees > 909090) {
                    milliseconds = degrees
                    degrees = 0.0
                } else if (degrees > 9090) {
                    val newDegrees = floor(degrees / 10000)
                    minutes = floor((degrees - newDegrees * 10000) / 100)
                    seconds = floor(degrees - newDegrees * 10000 - minutes * 100)
                    degrees = newDegrees
                } else if (degrees > 360) {
                    val newDegrees = floor(degrees / 100)
                    minutes = degrees - newDegrees * 100
                    degrees = newDegrees
                }
            }
            return sign * (degrees + minutes / 60 + seconds / 3600 + milliseconds / 3600000)
        }
    }
}
