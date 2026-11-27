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

import kotlin.js.Date

/**
 * Browser implementation is not finished yet: only UTC and the browser's own zone are supported.
 * TODO use Intl.DateTimeFormat for named zones when the library is used in web-ui.
 */
internal actual object TimeZones {

    private const val LOCAL = "local"

    actual fun systemZone(): String = LOCAL

    actual fun isValidZone(zone: String): Boolean = zone == "UTC"

    actual fun offsetMillis(zone: String, epochMillis: Long): Long =
        if (zone == "UTC") 0L else (-Date(epochMillis.toDouble()).getTimezoneOffset() * 60_000.0).toLong()

    actual fun wallToEpochMillis(zone: String, wallMillis: Long): Long {
        if (zone == "UTC") return wallMillis
        val utc = Date(wallMillis.toDouble())
        val local = Date(utc.getUTCFullYear(), utc.getUTCMonth(), utc.getUTCDate(), utc.getUTCHours(), utc.getUTCMinutes(), utc.getUTCSeconds(), utc.getUTCMilliseconds())
        return local.getTime().toLong()
    }

    actual fun zoneAbbreviation(zone: String, epochMillis: Long): String = if (zone == "UTC") "UTC" else ""

    actual fun currentTimeMillis(): Long = Date.now().toLong()
}
