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
package io.lumeer.datafilters.date

/** Platform time zone rules (java.time on the JVM). Offsets are milliseconds east of UTC. */
internal expect object TimeZones {
    fun systemZone(): String

    fun isValidZone(zone: String): Boolean

    fun offsetMillis(zone: String, epochMillis: Long): Long

    /** Resolves a wall clock time (given as epoch millis of the same fields in UTC): gaps move forward, overlaps take the earlier offset. */
    fun wallToEpochMillis(zone: String, wallMillis: Long): Long

    fun zoneAbbreviation(zone: String, epochMillis: Long): String

    fun currentTimeMillis(): Long
}
