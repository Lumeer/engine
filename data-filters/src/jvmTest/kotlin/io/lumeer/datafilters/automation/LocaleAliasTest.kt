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
package io.lumeer.datafilters.automation

import kotlin.test.Test
import kotlin.test.assertEquals

class LocaleAliasTest {

    private val dates = AutomationDates(zone = "UTC", now = { 0L })

    @Test
    fun czFormatsAsCs() {
        for (locale in listOf("cz", "CZ", "cz-CZ", "cz_cz")) {
            assertEquals("leden čtvrtek", dates.formatDate(0.0, "MMMM dddd", locale), locale)
        }
        assertEquals(dates.formatDate(0.0, "MMMM dddd", "cs"), dates.formatDate(0.0, "MMMM dddd", "cz"))
    }

    @Test
    fun czParsesAsCs() {
        assertEquals(dates.parseDate("1. ledna 1970", "D. MMMM YYYY", "cs"), dates.parseDate("1. ledna 1970", "D. MMMM YYYY", "cz"))
        assertEquals(0.0, dates.parseDate("1. ledna 1970", "D. MMMM YYYY", "cz"))
    }

    @Test
    fun otherLocalesUnchanged() {
        assertEquals("January Thursday", dates.formatDate(0.0, "MMMM dddd", "xx"))
        assertEquals("január štvrtok", dates.formatDate(0.0, "MMMM dddd", "sk"))
    }
}
