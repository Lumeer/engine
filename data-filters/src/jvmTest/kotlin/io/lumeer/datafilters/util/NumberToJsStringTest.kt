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
package io.lumeer.datafilters.util

import kotlin.test.Test
import kotlin.test.assertEquals

class NumberToJsStringTest {

    // expected values printed by node: String(Number(x))
    private val cases = listOf(
        "0".toDouble() to "0",
        "1".toDouble() to "1",
        "-1".toDouble() to "-1",
        "0.1".toDouble() to "0.1",
        "0.30000000000000004".toDouble() to "0.30000000000000004",
        "1e+21".toDouble() to "1e+21",
        "100000000000000000000".toDouble() to "100000000000000000000",
        "123456789012345680000".toDouble() to "123456789012345680000",
        "0.000001".toDouble() to "0.000001",
        "1e-7".toDouble() to "1e-7",
        "1.5e-7".toDouble() to "1.5e-7",
        "-1e-7".toDouble() to "-1e-7",
        "1234.5678".toDouble() to "1234.5678",
        "5e-324".toDouble() to "5e-324",
        "1.7976931348623157e+308".toDouble() to "1.7976931348623157e+308",
        "9007199254740994".toDouble() to "9007199254740994",
        "100".toDouble() to "100",
        "1e+300".toDouble() to "1e+300",
        "0.000001234".toDouble() to "0.000001234",
        "1.005".toDouble() to "1.005",
        "-0.000001".toDouble() to "-0.000001",
    )

    @Test
    fun matchesJsNumberToString() {
        for ((value, expected) in cases) {
            assertEquals(expected, numberToJsString(value), "value $value")
        }
        assertEquals("0", numberToJsString(-0.0))
        assertEquals("NaN", numberToJsString(Double.NaN))
        assertEquals("-Infinity", numberToJsString(Double.NEGATIVE_INFINITY))
    }
}
