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

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

internal actual object TimeZones {

    private val zones = ConcurrentHashMap<String, ZoneId>()
    private val abbreviationFormatter = DateTimeFormatter.ofPattern("zzz", Locale.ENGLISH)

    private fun zone(id: String): ZoneId = zones.getOrPut(id) { ZoneId.of(id) }

    actual fun systemZone(): String = ZoneId.systemDefault().id

    actual fun isValidZone(zone: String): Boolean = try {
        zone(zone)
        true
    } catch (e: Exception) {
        false
    }

    actual fun offsetMillis(zone: String, epochMillis: Long): Long =
        zone(zone).rules.getOffset(Instant.ofEpochMilli(epochMillis)).totalSeconds * 1000L

    actual fun wallToEpochMillis(zone: String, wallMillis: Long): Long {
        val local = LocalDateTime.ofInstant(Instant.ofEpochMilli(wallMillis), ZoneOffset.UTC)
        return ZonedDateTime.ofLocal(local, zone(zone), null).toInstant().toEpochMilli()
    }

    actual fun zoneAbbreviation(zone: String, epochMillis: Long): String =
        ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), zone(zone)).format(abbreviationFormatter)

    actual fun currentTimeMillis(): Long = System.currentTimeMillis()
}
